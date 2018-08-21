package hs.mediasystem.db;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.ext.basicmediatypes.scan.StreamPrint;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseStreamDataStore {
  private static final Logger LOGGER = Logger.getLogger(DatabaseStreamDataStore.class.getName());

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    .enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "@Class")
    .registerModule(new ParameterNamesModule(Mode.PROPERTIES))
    .registerModule(new JavaTimeModule())
    .registerModule(new RecordGroupModule());

  @Inject private Database database;

  @PostConstruct
  private void postConstruct() {
    // Test integrity of StreamData table:
    try(Transaction tx = database.beginTransaction()) {
      AtomicInteger count = new AtomicInteger();

      tx.select(StreamData.class).stream()
        .filter(sd -> toStreamPrint(sd).isException())
        .peek(sd -> count.incrementAndGet())
        .forEach(tx::delete);

      if(count.get() > 0) {
        tx.commit();
        LOGGER.warning("Removed " + count.get() + " malformed StreamData records");
      }
    }
  }

  public <T> Map<T, StreamPrint> findAll(Function<StreamPrint, T> keyFunction) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx.select(StreamData.class).stream()
        .map(DatabaseStreamDataStore::toStreamPrint)
        .flatMap(Exceptional::ignoreAllAndStream)
        .collect(Collectors.toMap(keyFunction, Function.identity()));
    }
  }

  public StreamPrint findByUrl(String url, long size, long lastModificationTime) {
    StreamData streamData = findStreamDataByUrl(url, size, lastModificationTime);

    return streamData == null ? null : toStreamPrint(streamData).handle(e -> LOGGER.warning("Exception while decoding StreamPrint: " + Throwables.formatAsOneLine(e))).orElse(null);
  }

  private StreamData findStreamDataByUrl(String url, Long size, long lastModificationTime) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      if(size == null) {
        return tx.selectUnique(StreamData.class, "url = ? AND size IS NULL AND modtime = ?", url, lastModificationTime);
      }

      return tx.selectUnique(StreamData.class, "url = ? AND size = ? AND modtime = ?", url, size, lastModificationTime);
    }
  }

  private StreamData findStreamDataByHash(byte[] hash, Long size, long lastModificationTime) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      if(size == null) {
        return tx.selectUnique(StreamData.class, "hash = ? AND size IS NULL AND modtime = ?", hash, lastModificationTime);
      }

      return tx.selectUnique(StreamData.class, "hash = ? AND size = ? AND modtime = ?", hash, size, lastModificationTime);
    }
  }

  public void store(StreamPrint streamPrint) {
    try(Transaction tx = database.beginTransaction()) {
      StreamData existingStreamData = findStreamDataByHash(streamPrint.getHash(), streamPrint.getSize(), streamPrint.getLastModificationTime());

      if(existingStreamData == null) {
        existingStreamData = findStreamDataByUrl(streamPrint.getUri().toString(), streamPrint.getSize(), streamPrint.getLastModificationTime());
      }

      StreamData streamData = existingStreamData == null ? new StreamData() : existingStreamData;

      streamData.setUrl(streamPrint.getUri().toString());
      streamData.setHash(streamPrint.getHash());
      streamData.setSize(streamPrint.getSize());
      streamData.setLastModificationTime(streamPrint.getLastModificationTime());
      streamData.setJson(OBJECT_MAPPER.writeValueAsBytes(streamPrint));

      tx.merge(streamData);
      tx.commit();
    }
    catch(JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Exceptional<StreamPrint> toStreamPrint(StreamData streamData) {
    try {
      return Exceptional.of(OBJECT_MAPPER.readValue(streamData.getJson(), StreamPrint.class));
    }
    catch(IOException e) {
      return Exceptional.ofException(e);
    }
  }
}
