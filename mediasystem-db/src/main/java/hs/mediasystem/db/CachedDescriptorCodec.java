package hs.mediasystem.db;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;

import java.io.IOException;
import java.time.Instant;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

@Singleton
public class CachedDescriptorCodec {
  private ObjectMapper objectMapper;

  @PostConstruct
  private void postConstruct() {
    objectMapper = new ObjectMapper()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
      .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
      .registerModule(new ParameterNamesModule(Mode.PROPERTIES))
      .registerModule(new BasicDataTypesModule())
      .registerModule(new MediaSystemDomainModule())
      .registerModule(new Jdk8Module())
      .registerModule(new JavaTimeModule());
  }

  public CachedDescriptor fromRecord(DescriptorRecord record) throws IOException {
    return new CachedDescriptor(Instant.ofEpochSecond(record.getLastUsedTime()), objectMapper.readValue(record.getJson(), MediaDescriptor.class));
  }

  public DescriptorRecord toRecord(CachedDescriptor descriptor) {
    try {
      DescriptorRecord record = new DescriptorRecord();

      record.setIdentifier(descriptor.getDescriptor().getIdentifier().toString());
      record.setLastUsedTime(descriptor.getLastUsedTime().getEpochSecond());
      record.setJson(objectMapper.writeValueAsBytes(descriptor.getDescriptor()));

      return record;
    }
    catch(JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
