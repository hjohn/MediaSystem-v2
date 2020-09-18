package hs.mediasystem.db.base;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CachedStreamCodec {
  @Inject private StreamDatabase database;

  private ObjectMapper objectMapper;

  @PostConstruct
  private void postConstruct() {
    objectMapper = new ObjectMapper()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
      .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
      .registerModule(new ParameterNamesModule(Mode.PROPERTIES))
      .registerModule(new JavaTimeModule())
      .registerModule(new Jdk8Module())
      .registerModule(new BasicDataTypesModule())
      .registerModule(new MediaSystemDomainModule());
  }

  public CachedStream fromRecord(StreamRecord record) throws IOException {
    Identification identification = record.getIdentifiers() == null || record.getIdentifiers().isEmpty() ? null
      : new Identification(
          record.getIdentifiers().stream().map(Identifier::fromString).collect(Collectors.toList()),
          new Match(Type.valueOf(record.getMatchType()), record.getMatchAccuracy(), Instant.ofEpochMilli(record.getMatchMillis()))
        );

    return new CachedStream(
      objectMapper.readValue(record.getJson(), Streamable.class),
      identification,
      Instant.ofEpochMilli(record.getCreationMillis()),
      record.getLastEnrichTime() == null ? null : Instant.ofEpochSecond(record.getLastEnrichTime()),
      record.getNextEnrichTime() == null ? null : Instant.ofEpochSecond(record.getNextEnrichTime())
    );
  }

  public StreamRecord toRecord(CachedStream stream) {
    try {
      StreamRecord record = new StreamRecord();
      StreamID id = stream.getStreamable().getId();

      database.find(id.getImportSourceId(), id.getContentId().asInt(), id.getName()).ifPresent(r -> record.setId(r.getId()));

      stream.getStreamable().getParentId().ifPresent(pid -> {
        record.setParentId(database.find(pid.getImportSourceId(), pid.getContentId().asInt(), pid.getName()).orElseThrow(() -> new IllegalStateException("parent not in database: " + pid)).getId());
      });

      record.setContentId(stream.getStreamable().getId().getContentId().asInt());
      record.setName(stream.getStreamable().getId().getName());
      record.setImportSourceId(stream.getStreamable().getId().getImportSourceId());
      record.setCreationMillis(stream.getDiscoveryTime().toEpochMilli());
      record.setLastEnrichTime(stream.getLastEnrichTime() == null ? null : stream.getLastEnrichTime().getEpochSecond());
      record.setNextEnrichTime(stream.getNextEnrichTime() == null ? null : stream.getNextEnrichTime().getEpochSecond());
      record.setJson(objectMapper.writeValueAsBytes(stream.getStreamable()));

      stream.getIdentification().ifPresent(i -> {
        record.setIdentifiers(i.getIdentifiers().stream().map(Object::toString).collect(Collectors.toList()));
        record.setMatchType(i.getMatch().getType().toString());
        record.setMatchAccuracy(i.getMatch().getAccuracy());
        record.setMatchMillis(i.getMatch().getCreationTime().toEpochMilli());
      });

      return record;
    }
    catch(JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
