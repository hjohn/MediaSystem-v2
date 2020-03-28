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
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.StringURI;

import java.io.IOException;

public class BasicDataTypesModule extends SimpleModule {

  public BasicDataTypesModule() {
    addSerializer(ImageURI.class, new JsonSerializer<ImageURI>() {
      @Override
      public void serialize(ImageURI value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
      }
    });

    addDeserializer(ImageURI.class, new JsonDeserializer<ImageURI>() {
      @Override
      public ImageURI deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return new ImageURI(p.getText());
      }
    });

    addSerializer(StringURI.class, new JsonSerializer<StringURI>() {
      @Override
      public void serialize(StringURI value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
      }
    });

    addDeserializer(StringURI.class, new JsonDeserializer<StringURI>() {
      @Override
      public StringURI deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return new StringURI(p.getText());
      }
    });

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
