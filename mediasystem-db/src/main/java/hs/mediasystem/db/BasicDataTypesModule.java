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

    addSerializer(StreamID.class, new JsonSerializer<StreamID>() {
      @Override
      public void serialize(StreamID value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeNumber(value.asInt());
      }
    });

    addDeserializer(StreamID.class, new JsonDeserializer<StreamID>() {
      @Override
      public StreamID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return new StreamID(p.getIntValue());
      }
    });
  }
}
