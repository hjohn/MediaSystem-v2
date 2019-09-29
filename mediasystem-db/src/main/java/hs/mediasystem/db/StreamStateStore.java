package hs.mediasystem.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.scanner.api.StreamID;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jdt.annotation.NonNull;

@Singleton
public class StreamStateStore {
  @Inject private Database database;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .registerModule(new Jdk8Module())
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  public void forEach(Consumer<@NonNull StreamState> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      tx.select(ssr -> consumer.accept(toStreamState(ssr)), StreamStateRecord.class);
    }
  }

  public void store(StreamState streamState) {
    StreamStateRecord record = toStreamStateRecord(streamState);

    try(Transaction tx = database.beginTransaction()) {
      StreamStateRecord existingRecord = tx.selectUnique(StreamStateRecord.class, "stream_id = ?", record.getStreamId());

      if(existingRecord == null) {
        tx.insert(record);
      }
      else {
        tx.update(record);
      }

      tx.commit();
    }
  }

  private static StreamStateRecord toStreamStateRecord(StreamState streamState) {
    try {
      StreamStateRecord record = new StreamStateRecord();

      record.setStreamId(streamState.getStreamID().asInt());
      record.setJson(OBJECT_MAPPER.writeValueAsBytes(streamState.getProperties()));

      return record;
    }
    catch(JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static @NonNull StreamState toStreamState(StreamStateRecord record) {
    try {
      return new StreamState(new StreamID(record.getStreamId()), OBJECT_MAPPER.readValue(record.getJson(), Map.class));
    }
    catch(IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
