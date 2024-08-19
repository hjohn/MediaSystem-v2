package hs.mediasystem.db.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import hs.mediasystem.domain.stream.ContentID;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.db.core.api.Database;
import org.int4.db.core.api.Transaction;
import org.int4.db.core.reflect.Reflector;

@Singleton
public class StreamStateStore {
  private static final Reflector<StreamStateRecord> ALL = Reflector.of(StreamStateRecord.class);

  @Inject private Database database;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .registerModule(new Jdk8Module())
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  public void forEach(Consumer<StreamState> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      tx."SELECT \{ALL} FROM streamstate"
        .map(ALL)
        .map(StreamStateStore::toStreamState)
        .consume(consumer::accept);
    }
  }

  public void store(StreamState streamState) {
    StreamStateRecord r = toStreamStateRecord(streamState);

    try(Transaction tx = database.beginTransaction()) {
      StreamStateRecord existingRecord = tx."SELECT \{ALL} FROM streamstate WHERE content_id = \{r.contentId()}"
        .map(ALL)
        .get();

      if(existingRecord == null) {
        tx."INSERT INTO streamstate (\{ALL}) VALUES (\{r})".execute();
      }
      else {
        tx."UPDATE streamstate SET \{ALL.entries(r)} WHERE content_id = \{r.contentId()}".execute();
      }

      tx.commit();
    }
  }

  private static StreamStateRecord toStreamStateRecord(StreamState streamState) {
    try {
      return new StreamStateRecord(
        streamState.getContentID().asInt(),
        OBJECT_MAPPER.writeValueAsBytes(streamState.getProperties())
      );
    }
    catch(JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static StreamState toStreamState(StreamStateRecord r) {
    try {
      return new StreamState(new ContentID(r.contentId()), OBJECT_MAPPER.readValue(r.json(), Map.class));
    }
    catch(IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
