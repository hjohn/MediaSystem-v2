package hs.mediasystem.db;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.scanner.api.StreamPrint;
import hs.mediasystem.scanner.api.StreamPrintProvider;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Treats StreamPrint as ID's.
 */
@Singleton
public class StreamPrintModule extends SimpleModule {
  @Inject private StreamPrintProvider streamPrintProvider;

  @PostConstruct
  private void postConstruct() {
    addSerializer(StreamPrint.class, new JsonSerializer<StreamPrint>() {
      @Override
      public void serialize(StreamPrint value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeNumber(value.getId().asInt());
      }
    });

    addDeserializer(StreamPrint.class, new JsonDeserializer<StreamPrint>() {
      @Override
      public StreamPrint deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return streamPrintProvider.get(new StreamID(p.getIntValue()));
      }
    });
  }
}
