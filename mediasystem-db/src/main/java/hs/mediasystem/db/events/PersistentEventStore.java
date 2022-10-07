package hs.mediasystem.db.events;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.database.core.Mapper;
import hs.mediasystem.util.events.EventStore;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
  private final Serializer<T> serializer;
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
   * @param storeName a name for the event store, cannot be {@code null} and must be a valid identifier
   * @param serializer a {@link Serializer}, cannot be {@code null}
   */
  public PersistentEventStore(Database database, String storeName, Serializer<T> serializer) {
    if(!STORE_NAME_PATTERN.matcher(Objects.requireNonNull(storeName, "storeName")).matches()) {
      throw new IllegalArgumentException("storeName must be a valid name: " + storeName);
    }

    this.database = Objects.requireNonNull(database, "database");
    this.serializer = Objects.requireNonNull(serializer, "serializer");
    this.tableName = "\"events_" + storeName + "\"";

    try(Transaction tx = database.beginTransaction()) {
      tx.execute(
        """
          CREATE TABLE IF NOT EXISTS "events_{storeName}" (
            id serial8,
            data bytea NOT NULL,

            CONSTRAINT "events_{storeName}_id" PRIMARY KEY (id)
          )
        """.replace("{storeName}", storeName)
      );

      Max max = tx.mapOne(MAX_MAPPER, "SELECT MAX(id) FROM " + tableName).orElseThrow();

      this.latestIndex = max.id == null ? -1 : max.id;

      tx.commit();
    }
  }

  @Override
  public long append(T event) {
    Objects.requireNonNull(event, "event");

    writeLock.lock();

    try {
      this.latestIndex = storeEvent(event);

      eventAvailableLatch.countDown();
      eventAvailableLatch = new CountDownLatch(1);

      return latestIndex;
    }
    finally {
      writeLock.unlock();
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
  public EventEnvelope<T> poll(long fromIndex) {
    if(fromIndex < 0) {
      throw new IllegalArgumentException("fromIndex must not be negative: " + fromIndex);
    }

    return fromIndex > latestIndex ? null : fetchEvent(fromIndex);
  }

  /**
   * Checks if this store has any events.
   *
   * @return {@code true} if there are events, otherwise {@code false}
   */
  public boolean hasEvents() {
    return latestIndex >= 0;
  }

  private long storeEvent(T event) {
    try(Transaction tx = database.beginTransaction()) {
      long id = tx.insert(tableName, Map.of("data", serializer.serialize(event)));

      tx.commit();

      return id;
    }
    catch(SerializerException e) {
      throw new IllegalStateException("Unable to serialize event: " + event, e);
    }
  }

  private EventEnvelope<T> fetchEvent(long fromIndex) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      EventRecord er = tx.mapOne(
        EVENT_RECORD_MAPPER,
        "SELECT id, data FROM " + tableName + " WHERE id >= ? ORDER BY id LIMIT 1",
        fromIndex
      ).orElseThrow();

      try {
        return new EventEnvelope<>(er.id, serializer.unserialize(er.data));
      }
      catch(SerializerException e) {
        throw new IllegalStateException("Unable to deserialize event: " + er, e);
      }
    }
  }
}