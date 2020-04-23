package hs.mediasystem.db.base;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.util.Attributes;

import java.io.IOException;
import java.util.Map;

public class MediaSystemDomainModule extends SimpleModule {

  public MediaSystemDomainModule() {
    addKeyDeserializer(Identifier.class, new KeyDeserializer() {
      @Override
      public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        return Identifier.fromString(key);
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
      public MediaType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
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
      public DataSource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return DataSource.fromString(p.getText());
      }
    });
  }

  @Override
  public void setupModule(SetupContext context) {
    super.setupModule(context);

    context.setMixInAnnotations(Attributes.class, AttributesMixIn.class);
    context.setMixInAnnotations(MediaDescriptor.class, MediaDescriptorMixIn.class);
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
}
