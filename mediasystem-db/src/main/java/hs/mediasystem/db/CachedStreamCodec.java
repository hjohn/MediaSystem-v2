package hs.mediasystem.db;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.time.Instant;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

@Singleton
public class CachedStreamCodec {
  private ObjectMapper objectMapper;

  @PostConstruct
  private void postConstruct() {
    objectMapper = new ObjectMapper()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
      .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
      .registerModule(new ParameterNamesModule(Mode.PROPERTIES))
      .registerModule(new JavaTimeModule())
      .registerModule(new MediaSystemDomainModule());
  }

  public CachedStream fromRecord(StreamRecord record) throws IOException {
    return new CachedStream(
      objectMapper.readValue(record.getJson(), IdentifiedStream.class),
      record.getScannerId(),
      record.getLastEnrichTime() == null ? null : Instant.ofEpochSecond(record.getLastEnrichTime()),
      record.getNextEnrichTime() == null ? null : Instant.ofEpochSecond(record.getNextEnrichTime())
    );
  }

  public StreamRecord toRecord(CachedStream stream) {
    try {
      StreamRecord record = new StreamRecord();

      record.setStreamId(stream.getIdentifiedStream().getStream().getId().asInt());
      record.setScannerId(stream.getScannerId());
      record.setLastEnrichTime(stream.getLastEnrichTime() == null ? null : stream.getLastEnrichTime().getEpochSecond());
      record.setNextEnrichTime(stream.getNextEnrichTime() == null ? null : stream.getNextEnrichTime().getEpochSecond());
      record.setJson(objectMapper.writeValueAsBytes(stream.getIdentifiedStream()));

      return record;
    }
    catch(JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
