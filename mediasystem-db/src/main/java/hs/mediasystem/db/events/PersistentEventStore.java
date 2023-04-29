package hs.mediasystem.db.events;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.database.core.Database.Transaction.TransactionState;
import hs.database.core.DatabaseException;
import hs.database.core.Mapper;
import hs.mediasystem.util.events.store.EventStore;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * An {@link EventStore} which stores all events in a {@link Database}.
 *
 * @param <T> the type of events stored by this event store
 */
public class PersistentEventStore<T> implements EventStore<T> {
  private static final Pattern STORE_NAME_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*");

  private final String storeName;
  private final Class<T> eventType;
  private final Store<T> store;

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final Lock readLock = lock.readLock();
  private final Lock writeLock = lock.writeLock();
  private final Lock taskLock = new ReentrantLock();
  private final Map<TaskKey, CompletableFuture<List<EventEnvelope<T>>>> sharedTasks = new ConcurrentHashMap<>();

  private CountDownLatch eventAvailableLatch = new CountDownLatch(1);
  private long latestIndex;

  /**
   * Constructs a new instance.
   *
   * @param database a {@link Database}, cannot be {@code null}
   * @param eventType the event {@link Class} stored in this store, cannot be {@code null}
   * @param serializer a {@link Serializer}, cannot be {@code null}
   */
  public PersistentEventStore(Database database, Class<T> eventType, EventSerializer<T> serializer) {
    this(database, eventType, eventType.getSimpleName(), serializer);
  }

  /**
   * Constructs a new instance.
   *
   * @param database a {@link Database}, cannot be {@code null}
   * @param eventType the event {@link Class} stored in this store, cannot be {@code null}
   * @param storeName a name for the event store, cannot be {@code null} and must be a valid identifier
   * @param serializer a {@link Serializer}, cannot be {@code null}
   */
  public PersistentEventStore(Database database, Class<T> eventType, String storeName, EventSerializer<T> serializer) {
    if(!STORE_NAME_PATTERN.matcher(Objects.requireNonNull(storeName, "storeName")).matches()) {
      throw new IllegalArgumentException("storeName must be a valid name: " + storeName);
    }

    this.eventType = Objects.requireNonNull(eventType, "eventType");
    this.storeName = storeName;
    this.store = new Store<>(Objects.requireNonNull(database, "database"), "events_" + storeName, serializer);
    this.latestIndex = store.initialize();
  }

  @Override
  public Class<T> eventType() {
    return eventType;
  }

  @Override
  public void append(Callback<T> callback, Consumer<Long> onSuccess) {
    Objects.requireNonNull(callback, "callback");
    Objects.requireNonNull(onSuccess, "onSuccess");

    // Don't allow concurrent appends until there is a solution for out of order id's and gaps
    writeLock.lock();

    try {
      store.storeEvents(callback, lastId -> {
        // Called when outer commit completes (successful or not)
        try {
          if(lastId != -1) {
            this.latestIndex = lastId;

            eventAvailableLatch.countDown();
            eventAvailableLatch = new CountDownLatch(1);

            onSuccess.accept(lastId);
          }
        }
        finally {
          writeLock.unlock();
        }
      });
    }
    catch(Exception e) {
      throw new IllegalStateException("Unable to serialize and store events", e);
    }
  }

  @Override
  public EventEnvelope<T> take(long fromIndex) throws InterruptedException {
    return take(fromIndex, 1).get(0);
  }

  @Override
  public List<EventEnvelope<T>> take(long fromIndex, int max) throws InterruptedException {
    if(fromIndex < 0) {
      throw new IllegalArgumentException("fromIndex must not be negative: " + fromIndex);
    }
    if(max <= 0) {
      throw new IllegalArgumentException("max must be positive: " + fromIndex);
    }

    for(;;) {
      CountDownLatch latch = null;

      readLock.lock();

      try {
        if(fromIndex > latestIndex) {
          latch = this.eventAvailableLatch;
        }
      }
      finally {
        readLock.unlock();
      }

      if(latch == null) {
        break;
      }

      latch.await();  // wait for new event to become available
    }

    return fetchEvents(fromIndex, max);
  }

  @Override
  public EventEnvelope<T> poll(long fromIndex) {
    List<EventEnvelope<T>> list = poll(fromIndex, 1);

    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public List<EventEnvelope<T>> poll(long fromIndex, int max) {
    if(fromIndex < 0) {
      throw new IllegalArgumentException("fromIndex must not be negative: " + fromIndex);
    }
    if(max <= 0) {
      throw new IllegalArgumentException("max must be positive: " + fromIndex);
    }

    return fromIndex > latestIndex ? List.of() : fetchEvents(fromIndex, max);
  }

  /**
   * Checks if this store has any events.
   *
   * @return {@code true} if there are events, otherwise {@code false}
   */
  public boolean hasEvents() {
    return latestIndex >= 0;
  }

  private List<EventEnvelope<T>> fetchEvents(long fromIndex, int max) {
    TaskKey key = new TaskKey(fromIndex, max);
    CompletableFuture<List<EventEnvelope<T>>> future;
    boolean triggered = true;

    taskLock.lock();

    try {
      future = sharedTasks.get(key);

      if(future == null) {
        triggered = false;
        future = new CompletableFuture<>();

        sharedTasks.put(key, future);
      }
    }
    finally {
      taskLock.unlock();
    }

    if(!triggered) {
      future.completeAsync(() -> store.queryEvents(fromIndex, max), Runnable::run);
    }

    try {
      return future.get();
    }
    catch(InterruptedException e) {
      throw new AssertionError(e);
    }
    catch(ExecutionException e) {
      if(e.getCause() instanceof RuntimeException re) {
        throw re;
      }

      throw new IllegalStateException(e.getCause());  // not expecting any checked exceptions
    }
    finally {
      sharedTasks.remove(key);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + storeName + "; latestIndex=" + latestIndex + "]";
  }

  private record TaskKey(long fromIndex, int max) {}

  private static class Store<T> {
    private static record Max(Long id) {}
    private static record EventRecord(long id, byte[] data) {}

    private static final Lookup lookup = MethodHandles.lookup();
    private static final Mapper<Max> MAX_MAPPER = Mapper.of(lookup, Max.class);
    private static final Mapper<EventRecord> EVENT_RECORD_MAPPER = Mapper.of(lookup, EventRecord.class);

    private final Database database;
    private final String tableName;
    private final String quotedTableName;
    private final Mapper<EventEnvelope<T>> mapper;
    private final EventSerializer<T> serializer;

    Store(Database database, String tableName, EventSerializer<T> serializer) {
      this.database = database;
      this.tableName = tableName;
      this.quotedTableName = "\"" + tableName + "\"";
      this.mapper = data -> createEnvelope(EVENT_RECORD_MAPPER.map(data));
      this.serializer = serializer;
    }

    long initialize() {
      try(Transaction tx = database.beginTransaction()) {
        tx.execute(
          """
            CREATE TABLE IF NOT EXISTS "{tableName}" (
              id SERIAL8,
              aggregate_id VARCHAR NOT NULL,
              type VARCHAR NOT NULL,
              data BYTEA NOT NULL,

              CONSTRAINT "{tableName}_id" PRIMARY KEY (id)
            )
          """.replace("{tableName}", tableName)
        );

        tx.execute(
          """
            CREATE INDEX IF NOT EXISTS "{tableName}_aggregate_idx"
              ON "{tableName}" (aggregate_id, type, id)
          """.replace("{tableName}", tableName)
        );

        Max max = tx.mapOne(MAX_MAPPER, "SELECT MAX(id) FROM " + quotedTableName).orElseThrow();

        tx.commit();

        return max.id == null ? -1 : max.id;
      }
    }

    List<EventEnvelope<T>> queryEvents(long fromIndex, int max) {
      try(Transaction tx = database.beginReadOnlyTransaction()) {
        return
          tx.mapAll(
            mapper,
            "SELECT id, data FROM " + quotedTableName + " WHERE id >= ? ORDER BY id LIMIT ?",
            fromIndex,
            max
          );
      }
    }

    void storeEvents(Callback<T> callback, Consumer<Long> completionHook) throws Exception {
      boolean completionHookSet = false;

      try(Transaction tx = database.beginTransaction()) {
        AtomicLong lastId = new AtomicLong();

        tx.addCompletionHook(transactionState -> completionHook.accept(transactionState == TransactionState.COMMITTED ? lastId.get() : -1));
        completionHookSet = true;

        callback.accept(event -> lastId.set(storeEventWithinTransaction(tx, event)));

        tx.commit();  // If an exception occurs in storing (serialization, database) this won't be reached
      }
      finally {
        if(!completionHookSet) {
          completionHook.accept(-1L);
        }
      }
    }

    private long storeEventWithinTransaction(Transaction tx, T event) throws DatabaseException, SerializerException {
      return tx.insert(quotedTableName, Map.of("aggregate_id", serializer.extractAggregateId(event), "type", serializer.extractType(event), "data", serializer.serialize(event)));
    }

    private EventEnvelope<T> createEnvelope(EventRecord er) {
      try {
        return new EventEnvelope<>(er.id, serializer.unserialize(er.data));
      }
      catch(SerializerException e) {
        throw new IllegalStateException("Unable to deserialize event: " + er, e);
      }
    }
  }
}