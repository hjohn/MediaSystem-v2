package hs.database.core;

import hs.database.util.Closer;
import hs.database.util.WeakValueMap;

import java.lang.reflect.InvocationTargetException;
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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Provider;

import org.eclipse.jdt.annotation.Nullable;

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
  void endTransaction() {
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
          this.connection = parent.getConnection();
          this.savepoint = connection.setSavepoint();

          parent.activeNestedTransactions++;
        }

        LOG.finer("New Transaction " + this);
      }
      catch(SQLException e) {
        throw new DatabaseException(this, "Exception while creating new transaction", e);
      }

      assert (this.parent != null && this.savepoint != null) || (this.parent == null && this.savepoint == null);
    }

    public Database getDatabase() {
      return Database.this;
    }

    public Provider<Connection> getConnectionProvider() {
      return connectionProvider;
    }

    public Connection getConnection() {
      return connection;
    }

    private void ensureNotFinished() {
      if(finished) {
        throw new IllegalStateException(this + ": Transaction already ended");
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

    public void associate(DatabaseObject obj) {
      @SuppressWarnings("unchecked")
      RecordMapper<DatabaseObject> recordMapper = (RecordMapper<DatabaseObject>)getRecordMapper(obj.getClass());

      associatedObjects.put(createAssociatedObjectId(obj.getClass(), recordMapper.extractIds(obj).values().toArray()), obj);
    }

    public DatabaseObject findAssociatedObject(Class<?> cls, Object[] ids) {
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

    public Record selectUnique(String fields, String tableName, String whereCondition, Map<String, Object> parameters) throws DatabaseException {
      List<Record> result = select(fields, tableName, whereCondition, parameters);

      return result.isEmpty() ? null : result.get(0);
    }

    public Record selectUnique(String fields, String tableName, String whereCondition, Object... parameters) throws DatabaseException {
      List<Record> result = select(fields, tableName, whereCondition, parameters);

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

    public List<Record> select(String fields, String tableName, String whereCondition, Map<String, Object> parameters) throws DatabaseException {
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

    public List<Record> select(String fields, String tableName, String whereCondition, Object... parameters) throws DatabaseException {
      List<Record> records = new ArrayList<>();

      ResultSetConsumer<FieldMapper> consumer = new ResultSetConsumer<>() {
        @Override
        public void accept(ResultSet rs, FieldMapper fieldMapper) throws SQLException {
          Object[] values = new Object[fieldMapper.getColumnCount()];

          for(int i = 1; i <= fieldMapper.getColumnCount(); i++) {
            values[i - 1] = rs.getObject(i);
          }

          records.add(new Record(values, fieldMapper));
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
    public <T> T selectUnique(Class<T> cls, String whereCondition, Object... parameters) throws DatabaseException {
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

    public <T> void select(Consumer<T> consumer, Class<T> cls) throws DatabaseException {
      select(consumer, cls, null);
    }

    public <T> List<T> select(Class<T> cls) throws DatabaseException {
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
    public <T> Optional<T> mapOne(Mapper<T> mapper, String sql, Object... parameters) throws DatabaseException {
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
    public <T> List<T> mapAll(Mapper<T> mapper, String sql, Object... parameters) throws DatabaseException {
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

    private <T> void query(ResultSetConsumer<T> consumer, Function<ResultSetMetaData, T> metaDataConverter, String sql, Object... parameters) {
      ensureNotFinished();

      LOG.fine(this + ": " + sql + ": " + Arrays.toString(parameters));

      try(PreparedStatement statement = connection.prepareStatement(sql)) {
        setParameters(Arrays.asList(parameters), statement);

        try(ResultSet rs = statement.executeQuery()) {
          T metaData = metaDataConverter.apply(rs.getMetaData());

          while(rs.next()) {
            consumer.accept(rs, metaData);
          }
        }
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

    public synchronized int execute(String sql) throws DatabaseException {
      try(PreparedStatement statement = connection.prepareStatement(sql)) {
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

    private void finishTransaction(boolean commit) throws DatabaseException {
      ensureNotFinished();

      if(activeNestedTransactions != 0) {
        throw new DatabaseException(this, "Attempt at rollback/commit while there are uncommitted nested transactions");
      }

      endTransaction();

      LOG.finer(this + (commit ? ": COMMIT" : ": ROLLBACK"));

      try {
        if(parent == null) {
          try {
            if(commit) {
              connection.commit();
            }
            else {
              connection.rollback();
            }
          }
          catch(SQLException e) {
            throw new DatabaseException(this, "Exception while committing/rolling back connection", e);
          }
          finally {
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
            parent.activeNestedTransactions--;
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
  }

  private static class QueryAndOrderedParameters {
    final String query;
    final Object[] arrayParameters;

    QueryAndOrderedParameters(String query, Object[] arrayParameters) {
      this.query = query;
      this.arrayParameters = arrayParameters;
    }
  }
}
