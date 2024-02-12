package hs.database.core;

import hs.database.core.Reflector.Entries;
import hs.database.core.Reflector.Values;
import hs.database.util.Closer;
import hs.database.util.WeakValueMap;

import java.lang.StringTemplate.Processor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Provider;

import org.eclipse.jdt.annotation.Nullable;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

public class Database {
  private static final Logger LOG = Logger.getLogger(Database.class.getName());
  private static final ThreadLocal<Transaction> CURRENT_TRANSACTION = new ThreadLocal<>();
  private static final Map<Class<?>, RecordMapper<?>> RECORD_MAPPERS = new HashMap<>();
  private static final Pattern NAMED_PARAMETER_PATTERN = Pattern.compile(":([a-zA-Z_]+)");

  private static long uniqueIdentifier;

  private final Provider<Connection> connectionProvider;

  public Database(Provider<Connection> connectionProvider) {
    this.connectionProvider = connectionProvider;
  }

  public Transaction beginTransaction() throws DatabaseException {
    return beginTransaction(false);
  }

  public Transaction beginReadOnlyTransaction() throws DatabaseException {
    return beginTransaction(true);
  }

  private Transaction beginTransaction(boolean readOnly) {
    Transaction transaction = new Transaction(CURRENT_TRANSACTION.get(), readOnly);

    CURRENT_TRANSACTION.set(transaction);

    return transaction;
  }

  @SuppressWarnings("resource")
  private static void endTransaction() {
    CURRENT_TRANSACTION.set(CURRENT_TRANSACTION.get().parent);
  }

  private static void setParameters(List<Object> parameterValues, PreparedStatement statement) throws SQLException {
    int parameterIndex = 1;

    for(Object value : parameterValues) {
      if(value instanceof Date d) {
        statement.setTimestamp(parameterIndex++, new Timestamp(d.getTime()));
      }
      else if(value instanceof LocalDate ld) {
        statement.setTimestamp(parameterIndex++, Timestamp.valueOf(ld.atStartOfDay()));
      }
      else if(value instanceof Enum<?> e) {
        statement.setObject(parameterIndex++, e.name());
      }
      else if(value instanceof Json) {
        statement.setObject(parameterIndex++, value.toString(), Types.OTHER);
      }
      else {
        statement.setObject(parameterIndex++, value);
      }
    }
  }

  public synchronized <T> Stream<T> stream(Class<T> cls, String where, Object... parameters) {
    return streamSelectInternal(cls, buildSelect(cls, where), parameters);
  }

  private synchronized <T> Stream<T> streamSelectInternal(Class<T> cls, String sql, Object... parameters) {
    LOG.fine(this + ": " + sql + ": " + Arrays.toString(parameters));

    Closer closer = new Closer();

    int characteristics = Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL;

    return StreamSupport.stream(() -> createSpliterator(closer, characteristics, cls, sql, parameters), characteristics, false)
      .onClose(() -> {
        try {
          closer.closeAll();
        }
        catch(Exception e) {
          throw new DatabaseException(null, sql + ": " + Arrays.toString(parameters), e);  // TODO null passed as transaction
        }
      });
  }

  @SuppressWarnings("resource")
  private <T> Spliterator<T> createSpliterator(Closer closer, int characteristics, Class<T> cls, String sql, Object... parameters) {
    Transaction tx = closer.add(beginReadOnlyTransaction());

    try {
      PreparedStatement statement = closer.add(tx.connection.prepareStatement(sql));

      setParameters(Arrays.asList(parameters), statement);

      ResultSet rs = closer.add(statement.executeQuery());
      ToInstanceConverter<T> converter = new ToInstanceConverter<>(tx, cls, rs);

      return new Spliterator<>() {
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
          tx.ensureNotFinished();

          try {
            boolean valid = converter.next();

            if(!valid) {
              try {
                closer.closeAll();
              }
              catch(Exception e) {
                throw new IllegalStateException(e);
              }

              return false;
            }

            action.accept(converter.toInstance());

            return true;
          }
          catch(SQLException e) {
            throw new DatabaseException(tx, "SQLException occured during streaming", e);
          }
        }

        @Override
        public Spliterator<T> trySplit() {
          return null;  // Can't split
        }

        @Override
        public long estimateSize() {
          return Long.MAX_VALUE;  // Unknown
        }

        @Override
        public int characteristics() {
          return characteristics;
        }
      };
    }
    catch(IllegalStateException | SQLException e) {
      try {
        closer.closeAll();
      }
      catch(Exception surpressed) {
        e.addSuppressed(surpressed);
      }

      throw new DatabaseException(tx, sql + ": " + Arrays.toString(parameters), e);
    }
  }

  private static class ToInstanceConverter<T> {
    private final Class<T> cls;
    private final RecordMapper<T> recordMapper;
    private final ResultSet rs;
    private final ResultSetMetaData metaData;
    private final Transaction tx;

    ToInstanceConverter(Transaction tx, Class<T> cls, ResultSet rs) throws SQLException {
      this.tx = tx;
      this.cls = cls;
      this.recordMapper = getRecordMapper(cls);
      this.rs = rs;
      this.metaData = rs.getMetaData();
    }

    public boolean next() throws SQLException {
      return rs.next();
    }

    public T toInstance() {
      try {
        Map<String, Object> values = new HashMap<>();

        for(int i = 1; i <= metaData.getColumnCount(); i++) {
          String columnName = metaData.getColumnName(i).toLowerCase();

          values.put(columnName, rs.getObject(i));
        }

        T record = cls.getDeclaredConstructor().newInstance();
        recordMapper.applyValues(tx, record, values);
        recordMapper.invokeAfterLoadStore(record, tx.getDatabase());  // TODO can probably be merged with applyValues now

        return record;
      }
      catch(IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
        throw new DatabaseException(tx, "Unable to instantiate class: " + cls, e);
      }
      catch(SQLException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private static <T> String buildSelect(Class<T> cls, String whereCondition) {
    return "SELECT * FROM " + getRecordMapper(cls).getTableName() + (whereCondition == null ? "" : " WHERE " + whereCondition);
  }

  public static <T> RecordMapper<T> getRecordMapper(Class<T> cls) {
    @SuppressWarnings("unchecked")
    RecordMapper<T> recordMapper = (RecordMapper<T>)RECORD_MAPPERS.get(cls);

    if(recordMapper == null) {
      recordMapper = AnnotatedRecordMapper.create(cls);

      RECORD_MAPPERS.put(cls, recordMapper);
    }

    return recordMapper;
  }

  public class Transaction implements AutoCloseable {
    interface ResultSetConsumer<T> {
      void accept(ResultSet resultSet, T metaData) throws SQLException;
    }

    private final Transaction parent;
    private final Connection connection;
    private final Savepoint savepoint;
    private final long id;
    private final boolean readOnly;

    private final WeakValueMap<String, DatabaseObject> associatedObjects = new WeakValueMap<>();
    private final List<Consumer<TransactionState>> completionHooks = new ArrayList<>();

    private int activeNestedTransactions;
    private boolean finished;

    Transaction(Transaction parent, boolean readOnly) throws DatabaseException {
      this.parent = parent;
      this.readOnly = readOnly;
      this.id = ++uniqueIdentifier;

      try {
        if(parent == null) {
          this.connection = connectionProvider.get();
          this.savepoint = null;

          connection.setAutoCommit(false);
        }
        else {
          this.connection = parent.connection;
          this.savepoint = connection.setSavepoint();

          parent.incrementNestedTransactions();
        }

        LOG.finer("New Transaction " + this);
      }
      catch(SQLException e) {
        throw new DatabaseException(this, "Exception while creating new transaction", e);
      }

      assert (this.parent != null && this.savepoint != null) || (this.parent == null && this.savepoint == null);
    }

    Database getDatabase() {
      return Database.this;
    }

    Provider<Connection> getConnectionProvider() {
      return connectionProvider;
    }

    private synchronized void incrementNestedTransactions() {
      activeNestedTransactions++;
    }

    private synchronized void decrementNestedTransactions() {
      activeNestedTransactions--;
    }

    private void ensureNotFinished() {
      if(finished) {
        throw new IllegalStateException(this + ": Transaction already ended");
      }
      if(activeNestedTransactions != 0) {
        throw new DatabaseException(this, "Using parent transaction while nested transactions are running is not supported");
      }
    }

    private void ensureNotReadOnly() {
      if(readOnly) {
        throw new DatabaseException(this, "Transaction is read only");
      }
    }

    private String createAssociatedObjectId(Class<?> cls, Object[] ids) {
      return cls.getName() + ":" + Arrays.toString(ids);
    }

    void associate(DatabaseObject obj) {
      @SuppressWarnings("unchecked")
      RecordMapper<DatabaseObject> recordMapper = (RecordMapper<DatabaseObject>)getRecordMapper(obj.getClass());

      associatedObjects.put(createAssociatedObjectId(obj.getClass(), recordMapper.extractIds(obj).values().toArray()), obj);
    }

    DatabaseObject findAssociatedObject(Class<?> cls, Object[] ids) {
      return associatedObjects.get(createAssociatedObjectId(cls, ids));
    }

    public synchronized long getDatabaseSize() throws DatabaseException {
      ensureNotFinished();

      String sql = "SELECT pg_database_size('mediasystem')";

      LOG.fine(this + ": " + sql);

      try(PreparedStatement statement = connection.prepareStatement(sql);
          ResultSet rs = statement.executeQuery()) {
        if(rs.next()) {
          return rs.getLong(1);
        }

        throw new DatabaseException(this, "Unable to get database size");
      }
      catch(SQLException e) {
        throw new DatabaseException(this, sql, e);
      }
    }

    public synchronized DatabaseRecord selectUnique(String fields, String tableName, String whereCondition, Map<String, Object> parameters) throws DatabaseException {
      List<DatabaseRecord> result = select(fields, tableName, whereCondition, parameters);

      return result.isEmpty() ? null : result.get(0);
    }

    public synchronized DatabaseRecord selectUnique(String fields, String tableName, String whereCondition, Object... parameters) throws DatabaseException {
      List<DatabaseRecord> result = select(fields, tableName, whereCondition, parameters);

      return result.isEmpty() ? null : result.get(0);
    }

    private QueryAndOrderedParameters createQueryAndOrderedParameters(String query, Map<String, Object> parameters) {
      StringBuffer queryBuilder = new StringBuffer();
      List<Object> orderedParameters = new ArrayList<>();

      Matcher matcher = NAMED_PARAMETER_PATTERN.matcher(query);

      while(matcher.find()) {
        if(!parameters.containsKey(matcher.group(1))) {
          throw new IllegalArgumentException("Named parameter '" + matcher.group(1) + "' missing: " + parameters);
        }

        orderedParameters.add(parameters.get(matcher.group(1)));
        matcher.appendReplacement(queryBuilder, "?");
      }

      matcher.appendTail(queryBuilder);

      return new QueryAndOrderedParameters(queryBuilder.toString(), orderedParameters.toArray(new Object[orderedParameters.size()]));
    }

    public synchronized List<DatabaseRecord> select(String fields, String tableName, String whereCondition, Map<String, Object> parameters) throws DatabaseException {
      QueryAndOrderedParameters queryAndOrderedParameters = createQueryAndOrderedParameters(whereCondition, parameters);

      return select(fields, tableName, queryAndOrderedParameters.query, queryAndOrderedParameters.arrayParameters);
    }

    private FieldMapper toFieldMapper(ResultSetMetaData metaData) {
      try {
        FieldMapper fieldMapper = new FieldMapper();

        for(int i = 0; i < metaData.getColumnCount(); i++) {
          fieldMapper.add(metaData.getTableName(i + 1).toLowerCase(), metaData.getColumnName(i + 1).toLowerCase(), i);
        }

        return fieldMapper;
      }
      catch(SQLException e) {
        throw new IllegalStateException(e);
      }
    }

    public synchronized List<DatabaseRecord> select(String fields, String tableName, String whereCondition, Object... parameters) throws DatabaseException {
      List<DatabaseRecord> records = new ArrayList<>();

      ResultSetConsumer<FieldMapper> consumer = new ResultSetConsumer<>() {
        @Override
        public void accept(ResultSet rs, FieldMapper fieldMapper) throws SQLException {
          Object[] values = new Object[fieldMapper.getColumnCount()];

          for(int i = 1; i <= fieldMapper.getColumnCount(); i++) {
            values[i - 1] = rs.getObject(i);
          }

          records.add(new DatabaseRecord(values, fieldMapper));
        }
      };

      String sql = "SELECT " + fields + " FROM " + tableName + (whereCondition == null ? "" : " WHERE " + whereCondition);

      query(consumer, this::toFieldMapper, sql, parameters);

      return records;
    }

    public <T> Optional<T> selectOptional(Class<T> cls, String whereCondition, Object... parameters) throws DatabaseException {
      List<T> result = select(cls, whereCondition, parameters);

      return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Nullable
    public synchronized <T> T selectUnique(Class<T> cls, String whereCondition, Object... parameters) throws DatabaseException {
      List<T> result = select(cls, whereCondition, parameters);

      return result.isEmpty() ? null : result.get(0);
    }

    public synchronized <T, R> List<R> select(Class<T> cls, Function<T, R> mapper, String whereCondition, Object... parameters) throws DatabaseException {
      List<R> records = new ArrayList<>();

      select(r -> records.add(mapper.apply(r)), cls, whereCondition, parameters);

      return records;
    }

    public synchronized <T> List<T> select(Class<T> cls, String whereCondition, Object... parameters) throws DatabaseException {
      return select(cls, Function.identity(), whereCondition, parameters);
    }

    private <T> T toInstance(Class<T> cls, RecordMapper<T> recordMapper, ResultSet rs, ResultSetMetaData metaData) {
      try {
        Map<String, Object> values = new HashMap<>();

        for(int i = 1; i <= metaData.getColumnCount(); i++) {
          String columnName = metaData.getColumnName(i).toLowerCase();

          values.put(columnName, rs.getObject(i));
        }

        T record = cls.getDeclaredConstructor().newInstance();
        recordMapper.applyValues(Transaction.this, record, values);
        recordMapper.invokeAfterLoadStore(record, Database.this);  // TODO can probably be merged with applyValues now

        return record;
      }
      catch(IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
        throw new DatabaseException(Transaction.this, "Unable to instantiate class: " + cls, e);
      }
      catch(SQLException e) {
        throw new IllegalStateException(e);
      }
    }

    public synchronized <T> void select(Consumer<T> consumer, Class<T> cls, String whereCondition, Object... parameters) throws DatabaseException {
      RecordMapper<T> recordMapper = getRecordMapper(cls);

      ResultSetConsumer<ResultSetMetaData> consumer2 = new ResultSetConsumer<>() {
        @Override
        public void accept(ResultSet rs, ResultSetMetaData metaData) {
          consumer.accept(toInstance(cls, recordMapper, rs, metaData));
        }
      };

      String sql = Database.buildSelect(cls, whereCondition);

      query(consumer2, Function.identity(), sql, parameters);
    }

    public synchronized List<Object[]> select(Class<?>[] classes, String[] aliases, String from, String whereCondition, Object... parameters) throws DatabaseException {
      List<Object[]> records = new ArrayList<>();

      ResultSetConsumer<ResultSetMetaData> consumer = new ResultSetConsumer<>() {
        @Override
        public void accept(ResultSet rs, ResultSetMetaData metaData) throws SQLException {
          try {
            Object[] tuple = new Object[classes.length];

            for(int j = 0; j < classes.length; j++) {
              Class<?> cls = classes[j];
              RecordMapper<?> recordMapper = getRecordMapper(cls);
              Map<String, Object> values = new HashMap<>();
              String prefix = recordMapper.getTableName() + "_";
              boolean hasNonNullField = false;

              for(int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i).toLowerCase();

                if(columnName.startsWith(prefix)) {
                  Object value = rs.getObject(i);

                  if(value != null) {
                    hasNonNullField = true;
                  }

                  values.put(columnName.substring(prefix.length()), value);
                }
              }

              if(hasNonNullField) {
                Object record = cls.getDeclaredConstructor().newInstance();

                recordMapper.applyValues(Transaction.this, record, values);
                recordMapper.invokeAfterLoadStore(record, Database.this);  // TODO can probably be merged with applyValues now

                tuple[j] = record;
              }
            }

            records.add(tuple);
          }
          catch(IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new DatabaseException(Transaction.this, "Unable to instantiate class", e);
          }
        }
      };

      String sql = buildSelect(classes, aliases, from, whereCondition);

      query(consumer, Function.identity(), sql, parameters);

      return records;
    }

    public synchronized <T> void select(Consumer<T> consumer, Class<T> cls) throws DatabaseException {
      select(consumer, cls, null);
    }

    public synchronized <T> List<T> select(Class<T> cls) throws DatabaseException {
      return select(cls, null);
    }

    /**
     * Executes the given SQL as a query returning a result set, and returns the
     * first result (if present) as a type T created by the given mapper function.
     *
     * @param <T> the type of the mapped results
     * @param mapper a mapper to map the results, cannot be {@code null}
     * @param sql a SQL string which results in a result set, cannot be {@code null}
     * @param parameters optional parameters
     * @return an optional mapped result, never {@code null}
     * @throws DatabaseException when an I/O error occurs
     */
    public synchronized <T> Optional<T> mapOne(Mapper<T> mapper, String sql, Object... parameters) throws DatabaseException {
      List<T> records = mapAll(
        Objects.requireNonNull(mapper, "mapper"),
        Objects.requireNonNull(sql, "sql"),
        parameters
      );

      return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    /**
     * Executes the given SQL as a query returning a result set, and returns all
     * results (if any) as a list of type T created by passing each result to
     * the given mapper function.
     *
     * @param <T> the type of the mapped results
     * @param mapper a mapper to map the results, cannot be {@code null}
     * @param sql a SQL string which results in a result set, cannot be {@code null}
     * @param parameters optional parameters
     * @return a list of mapped results, never {@code null} but can be empty
     * @throws DatabaseException when an I/O error occurs
     */
    public synchronized <T> List<T> mapAll(Mapper<T> mapper, String sql, Object... parameters) throws DatabaseException {
      Objects.requireNonNull(mapper, "mapper");
      Objects.requireNonNull(sql, "sql");

      List<T> records = new ArrayList<>();

      query(
        (rs, metaData) -> {
          Object[] data = new Object[metaData.getColumnCount()];

          for(int i = 1; i <= metaData.getColumnCount(); i++) {
            data[i - 1] = rs.getObject(i);
          }

          try {
            records.add(mapper.map(data));
          }
          catch(Throwable e) {
            throw new IllegalStateException("Mapping of result failed: " + Arrays.toString(data), e);
          }
        },
        Function.identity(),
        sql,
        parameters
      );

      return records;
    }

    public synchronized <T> List<T> copyAll(Mapper<T> mapper, String sql, List<Class<?>> columnTypes) throws DatabaseException {
      try {
        if(!connection.isWrapperFor(BaseConnection.class)) {
          return mapAll(mapper, sql);
        }

        return CopySupport.copyAll(this, createCopyManager(), mapper, sql, columnTypes);
      }
      catch(SQLException e) {
        throw new DatabaseException(this, sql, e);
      }
    }

    @SuppressWarnings("resource")
    private CopyManager createCopyManager() throws SQLException {
      return new CopyManager(connection.unwrap(BaseConnection.class));
    }

    private <T> void query(ResultSetConsumer<T> consumer, Function<ResultSetMetaData, T> metaDataConverter, String sql, Object... parameters) {
      ensureNotFinished();

      long nanos = System.nanoTime();

      try(PreparedStatement statement = connection.prepareStatement(sql)) {
        setParameters(Arrays.asList(parameters), statement);

        try(ResultSet rs = statement.executeQuery()) {
          T metaData = metaDataConverter.apply(rs.getMetaData());

          while(rs.next()) {
            consumer.accept(rs, metaData);
          }
        }

        LOG.fine(this + " [" + (System.nanoTime() - nanos) / 1000000 + " ms]: " + sql + ": " + Arrays.toString(parameters));
      }
      catch(IllegalStateException | SQLException e) {
        throw new DatabaseException(this, sql + ": " + Arrays.toString(parameters), e);
      }
    }

    private String buildSelect(Class<?>[] classes, String[] aliases, String from, String whereCondition) {
      StringBuilder fields = new StringBuilder();

      for(int j = 0; j < classes.length; j++) {
        Class<?> cls = classes[j];
        RecordMapper<?> recordMapper = getRecordMapper(cls);

        for(String columnName : recordMapper.getColumnNames()) {
          if(fields.length() > 0) {
            fields.append(", ");
          }
          fields.append(aliases[j]).append(".").append(columnName).append(" AS ").append(recordMapper.getTableName()).append("_").append(columnName);
        }
      }

      return "SELECT " + fields + " FROM " + from + (whereCondition == null ? "" : " WHERE " + whereCondition);
    }

    public synchronized int execute(String sql, Object... parameters) throws DatabaseException {
      try(PreparedStatement statement = connection.prepareStatement(sql)) {
        setParameters(Arrays.asList(parameters), statement);

        return statement.executeUpdate();
      }
      catch(SQLException e) {
        throw new DatabaseException(this, sql, e);
      }
    }

    /**
     * Merges a new or existing object into the database determined by whether or not the
     * object is transient.  Transient objects are objects which have no ID assigned yet.<p>
     *
     * Note: this only works for objects with generated ID's, assigning an ID to an object
     * manually will break the transient determination and this function may perform an
     * update on a non-existing object.
     *
     * @param <T> the type of the object
     * @param obj an object to merge with the database
     * @throws DatabaseException when a database exception occurs
     */
    public synchronized <T> void merge(T obj) throws DatabaseException {
      @SuppressWarnings("unchecked")
      RecordMapper<T> recordMapper = (RecordMapper<T>)getRecordMapper(obj.getClass());

      if(!recordMapper.hasIdColumn()) {
        throw new DatabaseException(this, "Cannot perform merge with object lacking a field annotated with @Id: " + obj);
      }

      if(recordMapper.isTransient(obj)) {
        insert(obj);
      }
      else {
        update(obj);
      }
    }

    public synchronized <T> void insert(T obj) throws DatabaseException {
      @SuppressWarnings("unchecked")
      RecordMapper<T> recordMapper = (RecordMapper<T>)getRecordMapper(obj.getClass());

      if(recordMapper.hasIdColumn()) {
        boolean isTransient = recordMapper.isTransient(obj);
        boolean idIsGenerated = recordMapper.isIdGenerated();

        if(isTransient && !idIsGenerated) {
          throw new DatabaseException(this, "Cannot perform insert with object that is transient when the @Id annotated field is not auto-generated: " + obj);
        }
        if(!isTransient && idIsGenerated) {
          throw new DatabaseException(this, "Cannot perform insert with object that is not transient when the @Id annotated field is auto-generated: " + obj);
        }
      }

      Map<String, Object> values = recordMapper.extractValues(obj);

      Object generatedKey = insert(recordMapper.getTableName(), values);

      if(generatedKey != null) {
        recordMapper.setGeneratedKey(obj, generatedKey);
      }
      recordMapper.invokeAfterLoadStore(obj, Database.this);
    }

    /**
     * Updates the given object, either completely or specific fields only.
     *
     * @param <T> the type of the object
     * @param obj an object to update
     * @param fieldNames the field names to update, or <code>null</code> for all fields
     * @throws DatabaseException when an I/O error occurs
     */
    public synchronized <T> void update(T obj, Set<String> fieldNames) throws DatabaseException {
      @SuppressWarnings("unchecked")
      RecordMapper<T> recordMapper = (RecordMapper<T>)getRecordMapper(obj.getClass());

      if(!recordMapper.hasIdColumn()) {
        throw new DatabaseException(this, "Cannot perform update with object lacking a field annotated with @Id: " + obj);
      }
      if(recordMapper.isTransient(obj)) {
        throw new DatabaseException(this, "Cannot perform update with an object that is transient: " + obj);
      }

      Map<String, Object> ids = recordMapper.extractIds(obj);
      Map<String, Object> values = recordMapper.extractValues(obj);

      if(fieldNames != null) {
        values.keySet().retainAll(fieldNames);
      }

      String whereCondition = "";
      Object[] parameters = new Object[ids.size()];
      int parameterIndex = 0;

      for(String id : ids.keySet()) {
        if(!whereCondition.isEmpty()) {
          whereCondition += " AND ";
        }
        whereCondition += id + " = ?";
        parameters[parameterIndex++] = ids.get(id);
      }

      update(recordMapper.getTableName(), values, whereCondition, parameters);

      recordMapper.invokeAfterLoadStore(obj, Database.this);
    }

    /**
     * Updates the given object.
     *
     * @param <T> the type of the object
     * @param obj an object to update
     * @throws DatabaseException when an I/O error occurs
     */
    public synchronized <T> void update(T obj) throws DatabaseException {
      update(obj, null);
    }

    public synchronized Object merge(String tableName, int id, Map<String, Object> parameters) throws DatabaseException {
      if(id == 0) {
        return insert(tableName, parameters);
      }

      update(tableName, id, parameters);

      return null;
    }

    public synchronized <T> T insert(String tableName, Map<String, Object> parameters) throws DatabaseException {
      ensureNotFinished();
      ensureNotReadOnly();

      StringBuilder fields = new StringBuilder();
      StringBuilder values = new StringBuilder();
      List<Object> parameterValues = new ArrayList<>();

      for(Map.Entry<String, Object> entry : parameters.entrySet()) {
        if(fields.length() > 0) {
          fields.append(",");
          values.append(",");
        }

        fields.append(entry.getKey());
        values.append("?");

        parameterValues.add(entry.getValue());
      }

      String sql = "INSERT INTO " + tableName + " (" + fields.toString() + ") VALUES (" + values.toString() + ")";

      LOG.fine(this + ": " + sql + ": " + parameters);

      try(PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        setParameters(parameterValues, statement);

        statement.execute();

        try(ResultSet rs = statement.getGeneratedKeys()) {
          if(rs.next()) {
            @SuppressWarnings("unchecked")
            T object = (T)rs.getObject(1);

            return object;
          }

          return null;
        }
      }
      catch(SQLException e) {
        if(e.getSQLState().startsWith("23")) {
          throw new ConstraintViolationException(this, "[" + e.getSQLState() + "] " + sql + ": " + parameters, e);
        }

        throw new DatabaseException(this, "[" + e.getSQLState() + "] " + sql + ": " + parameters, e);
      }
    }

    public synchronized int update(String tableName, int id, Map<String, Object> parameters) throws DatabaseException {
      return update(tableName, parameters, "id = ?", id);
    }

    public synchronized int update(String tableName, Map<String, Object> values, String whereCondition, Object... parameters) throws DatabaseException {
      ensureNotFinished();
      ensureNotReadOnly();

      StringBuilder set = new StringBuilder();
      List<Object> parameterValues = new ArrayList<>();

      for(Map.Entry<String, Object> entry : values.entrySet()) {
        if(set.length() > 0) {
          set.append(",");
        }

        set.append(entry.getKey());
        set.append("=?");

        parameterValues.add(entry.getValue());
      }

      String sql = "UPDATE " + tableName + " SET " + set.toString() + " WHERE " + whereCondition;

      LOG.fine(this + ": " + sql + ": " + Arrays.toString(parameters) + ": " + values);

      try(PreparedStatement statement = connection.prepareStatement(sql)) {
        setParameters(parameterValues, statement);
        int parameterIndex = values.size() + 1;

        for(Object o : parameters) {
          statement.setObject(parameterIndex++, o);
        }

        return statement.executeUpdate();
      }
      catch(SQLException e) {
        throw new DatabaseException(this, sql + ": " + Arrays.toString(parameters) + ": " + values, e);
      }
    }

    private boolean deleteById(String tableName, Map<String, Object> idMap) {
      String whereCondition = "";
      Object[] parameters = new Object[idMap.size()];
      int parameterIndex = 0;

      for(String id : idMap.keySet()) {
        if(!whereCondition.isEmpty()) {
          whereCondition += ", ";
        }
        whereCondition += id + " = ?";
        parameters[parameterIndex++] = idMap.get(id);
      }

      return delete(tableName, whereCondition, parameters) > 0;
    }

    public synchronized <T> boolean delete(Class<T> cls, Object... ids) {
      RecordMapper<T> recordMapper = getRecordMapper(cls);

      if(!recordMapper.hasIdColumn()) {
        throw new DatabaseException(this, "Cannot perform delete with object lacking a field annotated with @Id: " + cls);
      }

      return deleteById(recordMapper.getTableName(), recordMapper.associateIds(ids));
    }

    public synchronized <T> boolean delete(T obj) {
      @SuppressWarnings("unchecked")
      RecordMapper<T> recordMapper = (RecordMapper<T>)getRecordMapper(obj.getClass());

      if(!recordMapper.hasIdColumn()) {
        throw new DatabaseException(this, "Cannot perform delete with object lacking a field annotated with @Id: " + obj);
      }
      if(recordMapper.isTransient(obj)) {
        throw new DatabaseException(this, "Cannot perform delete with an object that is transient: " + obj);
      }

      return deleteById(recordMapper.getTableName(), recordMapper.extractIds(obj));
    }

    public synchronized int delete(String tableName, String whereCondition, Object... parameters) throws DatabaseException {
      ensureNotFinished();
      ensureNotReadOnly();

      String sql = "DELETE FROM " + tableName + " WHERE " + whereCondition;

      LOG.fine(this + ": " + sql + ": " + Arrays.toString(parameters));

      try(PreparedStatement statement = connection.prepareStatement(sql)) {
        int parameterIndex = 1;

        for(Object o : parameters) {
          statement.setObject(parameterIndex++, o);
        }

        return statement.executeUpdate();
      }
      catch(SQLException e) {
        throw new DatabaseException(this, sql + ": " + Arrays.toString(parameters), e);
      }
    }

    public synchronized int deleteChildren(String tableName, String parentTableName, long parentId) throws DatabaseException {
      ensureNotFinished();
      ensureNotReadOnly();

      String sql = "DELETE FROM " + tableName + " WHERE " + parentTableName + "_id = ?";

      LOG.fine(this + ": " + sql + ": [" + parentId + "]");

      try(PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, parentId);

        return statement.executeUpdate();
      }
      catch(SQLException e) {
        throw new DatabaseException(this, sql + ": [" + parentId + "]", e);
      }
    }

    /**
     * Executes one or more statements that can return multiple results. Use
     * the results object to get each result in turn.
     *
     * @param template an SQL template, cannot be {@code null}
     * @return a results object, never {@code null}
     * @throws DatabaseException when statements are invalid, or an I/O error occurred
     */
    public synchronized Results execute(StringTemplate template) {
      return new Results(template);
    }

    /**
     * Creates a query executor suitable for a single query type statement.
     *
     * @param template an SQL template, cannot be {@code null}
     * @return a query executor, never {@code null}
     */
    public synchronized QueryExecutor query(StringTemplate template) {
      return new QueryExecutor(template);
    }

    /**
     * Executes a single insert type statement of the form {@code INSERT INTO <table> (<field list>) VALUES (<value list>)}
     * <p>
     * The field list must consist of:
     * <ul>
     * <li>Field names of the given table</li>
     * <li>{@link Reflector}s which can provide field names of the given table</li>
     * </ul>
     * The value list must consist of:
     * <ul>
     * <li>Strings, arrays, primitive types or their boxes</li>
     * <li>{@link Record}s which values are used in their declared order</li>
     * <li>{@link Reflector.Values} which values are used in their declared order</li>
     * </ul>
     * <p>
     * Note: to ensure the statement is not rolled back at the end
     * of the transaction, {@link Transaction#commit()} should be called.
     *
     * @param template an SQL template, cannot be {@code null}
     * @return the number of affected rows, never negative
     * @throws DatabaseException when statements are invalid, or an I/O error occurred
     */
    public synchronized long executeInsert(StringTemplate template) {
      try (Results r = new Results(template)) {
        return r.getRowCount();
      }
    }

    public synchronized long executeUpdate(StringTemplate template) {
      try (Results r = new Results(template)) {
        return r.getRowCount();
      }
    }

    /**
     * Executes a single delete type statement.
     * <p>
     * Note: to ensure the statement is not rolled back at the end
     * of the transaction, {@link Transaction#commit()} should be called.
     *
     * @param template an SQL template, cannot be {@code null}
     * @return the number of affected rows, never negative
     * @throws DatabaseException when statements are invalid, or an I/O error occurred
     */
    public synchronized long executeDelete(StringTemplate template) {
      try (Results r = new Results(template)) {
        return r.getRowCount();
      }
    }

    public enum TransactionState {
      COMMITTED, ROLLED_BACK;
    }

    /**
     * Adds a completion hook which is called when the outer most transaction
     * completes. The passed {@link TransactionState} is never {@code null} and
     * indicates whether the transaction was committed or rolled back.
     *
     * @param consumer a consumer that is called after the outer most transaction completes, cannot be {@code null}
     */
    public synchronized void addCompletionHook(Consumer<TransactionState> consumer) {
      if(parent == null) {
        completionHooks.add(consumer);
      }
      else {
        parent.addCompletionHook(consumer);
      }
    }

    private void finishTransaction(boolean commit) throws DatabaseException {
      ensureNotFinished();

      endTransaction();

      LOG.finer(this + (commit ? ": COMMIT" : ": ROLLBACK"));

      try {
        if(parent == null) {
          boolean committed = false;

          try {
            if(commit) {
              connection.commit();

              committed = true;
            }
            else {
              connection.rollback();
            }
          }
          catch(SQLException e) {
            throw new DatabaseException(this, "Exception while committing/rolling back connection", e);
          }
          finally {
            for(Consumer<TransactionState> consumer : completionHooks) {
              try {
                consumer.accept(committed ? TransactionState.COMMITTED : TransactionState.ROLLED_BACK);
              }
              catch(Exception e) {
                LOG.log(Level.WARNING, "Commit hook for " + this + " threw exception: " + consumer, e);
              }
            }

            completionHooks.clear();

            try {
              connection.close();
            }
            catch(SQLException e) {
              LOG.fine(this + ": exception while closing connection: " + e);
            }
          }
        }
        else {
          try {
            if(commit) {
              connection.releaseSavepoint(savepoint);
            }
            else {
              connection.rollback(savepoint);
            }
          }
          catch(SQLException e) {
            throw new DatabaseException(this, "Exception while finishing nested transaction", e);
          }
          finally {
            parent.decrementNestedTransactions();
          }
        }
      }
      finally {
        finished = true;
      }
    }

    public synchronized void commit() throws DatabaseException {
      finishTransaction(true);
    }

    public synchronized void rollback() throws DatabaseException {
      finishTransaction(false);
    }

    @Override
    public String toString() {
      return String.format("T%04d%s", id, parent == null ? "" : " (" + parent + ")");
    }

    @Override
    public void close() {
      if(!finished) {
        if(readOnly) {
          commit();
        }
        else {
          rollback();
        }
      }
    }

    abstract class Base {
      private static final SqlMapper<String> TEXT_MAPPER = rs -> rs.getString(1);
      private static final SqlMapper<Integer> INT_MAPPER = rs -> rs.getInt(1);
      private static final SqlMapper<Long> LONG_MAPPER = rs -> rs.getLong(1);
      private static final SqlMapper<byte[]> BYTES_MAPPER = rs -> rs.getBytes(1);

      public abstract <T> T as(SqlMapper<T> mapper);
      public abstract <T> List<T> asList(SqlMapper<T> mapper);
      public abstract <T> void consume(SqlMapper<T> mapper, Consumer<T> consumer);

      public final <T> Optional<T> asOptional(SqlMapper<T> mapper) {
        return Optional.ofNullable(as(mapper));
      }

      public final String asText() {
        return as(TEXT_MAPPER);
      }

      public final Optional<String> asOptionalText() {
        return asOptional(TEXT_MAPPER);
      }

      public final int asInt() {
        return as(INT_MAPPER);
      }

      public final Optional<Integer> asOptionalInt() {
        return asOptional(INT_MAPPER);
      }

      public final long asLong() {
        return as(LONG_MAPPER);
      }

      public final Optional<Long> asOptionalLong() {
        return asOptional(LONG_MAPPER);
      }

      public final byte[] asBytes() {
        return as(BYTES_MAPPER);
      }

      public final Optional<byte[]> asOptionalBytes() {
        return asOptional(BYTES_MAPPER);
      }
    }

    public class Results extends Base implements AutoCloseable {
      private final PreparedStatement ps;

      enum State {RESULT_SET, UPDATE_COUNT}

      private State state;
      private long updateCount = -1;

      Results(StringTemplate template) {
        try {
          this.ps = toPreparedStatement(connection, template);

          try {
            this.state = ps.execute() ? State.RESULT_SET : (updateCount = ps.getLargeUpdateCount()) == -1 ? null : State.UPDATE_COUNT;
          }
          catch(SQLException e) {
            throw new DatabaseException(Transaction.this, "execution failed for:\n    " + template + "\n    --> " + ps.toString(), e);
          }
        }
        catch(SQLException e) {
          throw new DatabaseException(Transaction.this, "creating statement failed for: " + template, e);
        }
      }

      @Override
      public void close() {
        try {
          ps.close();
        }
        catch(SQLException e) {
          throw new DatabaseException(Transaction.this, "close failed", e);
        }
      }

      private void ensureIsResultSet() {
        if(state != State.RESULT_SET) {
          throw new DatabaseException(Transaction.this, "not a result set");
        }
      }

      private void ensureIsUpdateCount() {
        if(state != State.UPDATE_COUNT) {
          throw new DatabaseException(Transaction.this, "not an update count");
        }
      }

      private void moveToNextResult() throws SQLException {
        this.state = ps.getMoreResults() ? State.RESULT_SET : (updateCount = ps.getLargeUpdateCount()) == -1 ? null : State.UPDATE_COUNT;
      }

      public long getRowCount() {
        ensureIsUpdateCount();

        try {
          long count = updateCount;

          moveToNextResult();

          return count;
        }
        catch(SQLException e) {
          throw new DatabaseException(Transaction.this, "error getting update count", e);
        }
      }

      @Override
      public <T> List<T> asList(SqlMapper<T> mapper) {
        ensureIsResultSet();

        try(ResultSet rs = ps.getResultSet()) {
          RestrictedResultSet restrictedResultSet = new RestrictedResultSet(rs);
          List<T> results = new ArrayList<>();

          while(rs.next()) {
            results.add(mapper.map(restrictedResultSet));
          }

          moveToNextResult();

          return results;
        }
        catch(SQLException e) {
          throw new DatabaseException(Transaction.this, e.getMessage(), e);
        }
      }

      @Override
      public <T> T as(SqlMapper<T> mapper) {
        ensureIsResultSet();

        try(ResultSet rs = ps.getResultSet()) {
          T element = rs.next() ? mapper.map(new RestrictedResultSet(rs)) : null;

          moveToNextResult();

          return element;
        }
        catch(SQLException e) {
          throw new DatabaseException(Transaction.this, e.getMessage(), e);
        }
      }

      @Override
      public <T> void consume(SqlMapper<T> mapper, Consumer<T> consumer) {
        ensureIsResultSet();

        try(ResultSet rs = ps.getResultSet()) {
          RestrictedResultSet restrictedResultSet = new RestrictedResultSet(rs);

          while(rs.next()) {
            consumer.accept(mapper.map(restrictedResultSet));
          }

          moveToNextResult();
        }
        catch(SQLException e) {
          throw new DatabaseException(Transaction.this, e.getMessage(), e);
        }
      }
    }

    public class QueryExecutor extends Base {
      private final StringTemplate template;

      QueryExecutor(StringTemplate template) {
        this.template = template;
      }

      @Override
      public <T> List<T> asList(SqlMapper<T> mapper) {
        try(Results r = new Results(template)) {
          return r.asList(mapper);
        }
      }

      @Override
      public <T> T as(SqlMapper<T> mapper) {
        try(Results r = new Results(template)) {
          return r.as(mapper);
        }
      }

      @Override
      public <T> void consume(SqlMapper<T> mapper, Consumer<T> consumer) {
        try(Results r = new Results(template)) {
          r.consume(mapper, consumer);
        }
      }
    }
  }

  private static class QueryAndOrderedParameters {
    final String query;
    final Object[] arrayParameters;

    QueryAndOrderedParameters(String query, Object[] arrayParameters) {
      this.query = query;
      this.arrayParameters = arrayParameters;
    }
  }

  private static final Predicate<String> NOT_EMPTY = Predicate.not(String::isEmpty);

  private static PreparedStatement toPreparedStatement(Connection connection, StringTemplate template) throws SQLException {
    StringBuilder sb = new StringBuilder();
    List<String> fragments = template.fragments();
    List<Object> values = template.values();

    for(int i = 0; i < values.size(); i++) {
      Object value = values.get(i);

      sb.append(fragments.get(i));

      if(value instanceof Reflector r) {
        sb.append(r.names().stream().filter(NOT_EMPTY).collect(Collectors.joining(", ")));
      }
      else if(value instanceof Entries e) {
        sb.append(e.reflector().names().stream().filter(NOT_EMPTY).map(t -> t + " = ?").collect(Collectors.joining(", ")));
      }
      else if(value instanceof Values v) {
        sb.append(v.reflector().names().stream().filter(NOT_EMPTY).map(t -> "?").collect(Collectors.joining(", ")));
      }
      else if(value instanceof Record r) {
        RecordComponent[] recordComponents = r.getClass().getRecordComponents();

        for (int j = 0; j < recordComponents.length; j++) {
          if (j != 0) {
            sb.append(",");
          }

          sb.append("?");
        }
      }
      else {
        sb.append("?");
      }
    }

    sb.append(fragments.getLast());

    PreparedStatement ps = connection.prepareStatement(sb.toString());

    fillParameters(ps, template.values());

    return ps;
  }

  private static void fillParameters(PreparedStatement ps, List<Object> values) throws SQLException {
    int index = 1;

    for(Object value : values) {
      index = fillParameter(index, ps, value);
    }
  }

  private static int fillParameter(int startIndex, PreparedStatement ps, Object value) throws SQLException {
    int index = startIndex;

    switch(value) {
      case Entries e -> {
        Record data = e.data();
        RecordComponent[] recordComponents = data.getClass().getRecordComponents();
        List<String> names = e.reflector().names();

        for(int i = 0; i < names.size(); i++) {
          String name = names.get(i);

          if(!name.isEmpty()) {
            try {
              index = fillParameter(index, ps, recordComponents[i].getAccessor().invoke(data));
            }
            catch(IllegalAccessException | InvocationTargetException ex) {
              throw new IllegalStateException(ex);
            }
          }
        }
      }
      case Values v -> {
        Record data = v.data();
        RecordComponent[] recordComponents = data.getClass().getRecordComponents();
        List<String> names = v.reflector().names();

        for(int i = 0; i < names.size(); i++) {
          String name = names.get(i);

          if(!name.isEmpty()) {
            try {
              index = fillParameter(index, ps, recordComponents[i].getAccessor().invoke(data));
            }
            catch(IllegalAccessException | InvocationTargetException ex) {
              throw new IllegalStateException(ex);
            }
          }
        }
      }
      case Reflector r -> {}
      case Record data -> {
        for(RecordComponent recordComponent : data.getClass().getRecordComponents()) {
          try {
            index = fillParameter(index, ps, recordComponent.getAccessor().invoke(data));
          }
          catch(IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
          }
        }
      }
      case Integer i -> ps.setInt(index++, i);
      case Float f -> ps.setFloat(index++, f);
      case Double d -> ps.setDouble(index++, d);
      case Boolean b -> ps.setBoolean(index++, b);
      case String s -> ps.setString(index++, s);
      case byte[] ba -> ps.setBytes(index++, ba);
      default -> throw new UnsupportedOperationException("unknown type for value: " + value + " at index " + index + "; type: " + value.getClass());
    }

    return index;
  }

  public static final Processor<StringTemplate, RuntimeException> SQL = StringTemplate.RAW;
}
