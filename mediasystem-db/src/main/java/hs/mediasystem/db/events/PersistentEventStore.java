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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
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

  private static record Max(Long id) {}
  private static record EventRecord(long id, byte[] data) {}

  private static final Lookup lookup = MethodHandles.lookup();
  private static final Mapper<Max> MAX_MAPPER = Mapper.of(lookup, Max.class);
  private static final Mapper<EventRecord> EVENT_RECORD_MAPPER = Mapper.of(lookup, EventRecord.class);

  private final Database database;
  private final Class<T> eventType;
  private final EventSerializer<T> serializer;
  private final String tableName;

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final Lock readLock = lock.readLock();
  private final Lock writeLock = lock.writeLock();

  private long latestIndex;

  private CountDownLatch eventAvailableLatch = new CountDownLatch(1);

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

    this.database = Objects.requireNonNull(database, "database");
    this.eventType = Objects.requireNonNull(eventType, "eventType");
    this.serializer = Objects.requireNonNull(serializer, "serializer");
    this.tableName = "\"events_" + storeName + "\"";

    try(Transaction tx = database.beginTransaction()) {
      tx.execute(
        """
          CREATE TABLE IF NOT EXISTS "events_{storeName}" (
            id SERIAL8,
            aggregate_id VARCHAR NOT NULL,
            type VARCHAR NOT NULL,
            data BYTEA NOT NULL,

            CONSTRAINT "events_{storeName}_id" PRIMARY KEY (id)
          )
        """.replace("{storeName}", storeName)
      );

      tx.execute(
        """
          CREATE INDEX IF NOT EXISTS "events_{storeName}_aggregate_idx"
            ON "events_{storeName}" (aggregate_id, type, id)
        """.replace("{storeName}", storeName)
      );

      Max max = tx.mapOne(MAX_MAPPER, "SELECT MAX(id) FROM " + tableName).orElseThrow();

      this.latestIndex = max.id == null ? -1 : max.id;

      tx.commit();
    }
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
      storeEvents(callback, lastId -> {
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
    if(fromIndex < 0) {
      throw new IllegalArgumentException("fromIndex must not be negative: " + fromIndex);
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

    return fetchEvent(fromIndex);
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
    if(fromIndex < 0) {
      throw new IllegalArgumentException("fromIndex must not be negative: " + fromIndex);
    }

    return fromIndex > latestIndex ? null : fetchEvent(fromIndex);
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

  private void storeEvents(Callback<T> callback, Consumer<Long> completionHook) throws Exception {
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
      return tx.insert(tableName, Map.of("aggregate_id", serializer.extractAggregateId(event), "type", serializer.extractType(event), "data", serializer.serialize(event)));
  }

  private EventEnvelope<T> fetchEvent(long fromIndex) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      EventRecord er = tx.mapOne(
        EVENT_RECORD_MAPPER,
        "SELECT id, data FROM " + tableName + " WHERE id >= ? ORDER BY id LIMIT 1",
        fromIndex
      ).orElseThrow(() -> new IllegalStateException("No records in " + tableName + " matched id >= " + fromIndex));

      return createEnvelope(er);
    }
  }

  private List<EventEnvelope<T>> fetchEvents(long fromIndex, int max) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return
        tx.mapAll(
          EVENT_RECORD_MAPPER,
          "SELECT id, data FROM " + tableName + " WHERE id >= ? ORDER BY id LIMIT ?",
          fromIndex,
          max
        )
        .stream()
        .map(this::createEnvelope)
        .toList();
    }
  }

  private EventEnvelope<T> createEnvelope(EventRecord er) {
    try {
      return new EventEnvelope<>(er.id, serializer.unserialize(er.data));
    }
    catch(SerializerException e) {
      throw new IllegalStateException("Unable to deserialize event: " + er, e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + tableName + "; latestIndex=" + latestIndex + "]";
  }
}