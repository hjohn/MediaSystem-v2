package hs.mediasystem.db.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import hs.mediasystem.domain.stream.ContentID;

import java.io.IOException;

public class BasicDataTypesModule extends SimpleModule {

  public BasicDataTypesModule() {
    addSerializer(ContentID.class, new JsonSerializer<ContentID>() {
      @Override
      public void serialize(ContentID value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeNumber(value.asInt());
      }
    });

    addDeserializer(ContentID.class, new JsonDeserializer<ContentID>() {
      @Override
      public ContentID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return new ContentID(p.getIntValue());
      }
    });
  }
}
