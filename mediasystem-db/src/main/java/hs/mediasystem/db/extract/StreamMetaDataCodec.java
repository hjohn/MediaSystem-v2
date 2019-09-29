package hs.mediasystem.db.extract;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.db.BasicDataTypesModule;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

@Singleton
public class StreamMetaDataCodec {
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
      .registerModule(new BasicDataTypesModule());
  }

  public byte[] encode(StreamMetaData input) throws JsonProcessingException {
    return objectMapper.writeValueAsBytes(input);
  }

  public StreamMetaData decode(byte[] json) throws JsonParseException, JsonMappingException, IOException {
    return objectMapper.readValue(json, StreamMetaData.class);
  }
}
