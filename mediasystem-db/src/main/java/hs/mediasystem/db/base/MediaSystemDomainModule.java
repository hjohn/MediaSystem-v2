package hs.mediasystem.db.base;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.KeywordId;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.Attributes;

import java.io.IOException;
import java.util.Map;

public class MediaSystemDomainModule extends SimpleModule {

  public MediaSystemDomainModule() {
    addKeyDeserializer(WorkId.class, new KeyDeserializer() {
      @Override
      public Object deserializeKey(String key, DeserializationContext ctxt) {
        return workIdFromString(key);
      }
    });

    addSerializer(WorkId.class, new JsonSerializer<WorkId>() {
      @Override
      public void serialize(WorkId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
      }
    });

    addDeserializer(WorkId.class, new JsonDeserializer<WorkId>() {
      @Override
      public WorkId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return workIdFromString(p.getText());
      }
    });

    addSerializer(KeywordId.class, new JsonSerializer<KeywordId>() {
      @Override
      public void serialize(KeywordId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
      }
    });

    addDeserializer(KeywordId.class, new JsonDeserializer<KeywordId>() {
      @Override
      public KeywordId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return keywordIdFromString(p.getText());
      }
    });

    addSerializer(MediaType.class, new JsonSerializer<MediaType>() {
      @Override
      public void serialize(MediaType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
      }
    });

    addDeserializer(MediaType.class, new JsonDeserializer<MediaType>() {
      @Override
      public MediaType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return MediaType.valueOf(p.getText());
      }
    });

    addSerializer(DataSource.class, new JsonSerializer<DataSource>() {
      @Override
      public void serialize(DataSource value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
      }
    });

    addDeserializer(DataSource.class, new JsonDeserializer<DataSource>() {
      @Override
      public DataSource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return DataSource.instance(p.getText());
      }
    });
  }

  @Override
  public void setupModule(SetupContext context) {
    super.setupModule(context);

    context.setMixInAnnotations(Attributes.class, AttributesMixIn.class);
    context.setMixInAnnotations(WorkDescriptor.class, MediaDescriptorMixIn.class);
  }

  static class AttributesMixIn extends Attributes {
    @JsonCreator
    public AttributesMixIn(Map<String, Object> attributes) {
      super(attributes);
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
  static class MediaDescriptorMixIn {
  }

  private static WorkId workIdFromString(String key) {
    int colon1 = key.indexOf(':');
    int colon2 = key.indexOf(':', colon1 + 1);

    return new WorkId(DataSource.instance(key.substring(0, colon1)), MediaType.valueOf(key.substring(colon1 + 1, colon2)), key.substring(colon2 + 1));
  }

  private static KeywordId keywordIdFromString(String key) {
    int colon = key.indexOf(':');

    return new KeywordId(DataSource.instance(key.substring(0, colon)), key.substring(colon + 1));
  }
}
