package hs.mediasystem.db;

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
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.EpisodeStream;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.MovieStream;
import hs.mediasystem.ext.basicmediatypes.SerieStream;
import hs.mediasystem.ext.basicmediatypes.StreamPrint;
import hs.mediasystem.ext.basicmediatypes.Type;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.StringURI;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

public class RecordGroupModule extends SimpleModule {
  @Inject
  public RecordGroupModule() {
    addKeyDeserializer(Identifier.class, new KeyDeserializer() {
      @Override
      public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        return Identifier.fromString(key);
      }
    });

    addSerializer(Type.class, new JsonSerializer<Type>() {
      @Override
      public void serialize(Type value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
      }
    });

    addDeserializer(Type.class, new JsonDeserializer<Type>() {
      @Override
      public Type deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return Type.of(p.getText());
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
  }

  @Override
  public void setupModule(SetupContext context) {
    registerSubtypes(
      new NamedType(MovieStream.class, "MovieStream"),
      new NamedType(SerieStream.class, "SerieStream"),
      new NamedType(EpisodeStream.class, "EpisodeStream")
    );

    super.setupModule(context);

    context.setMixInAnnotations(StreamPrint.class, StreamPrintMixIn.class);
    context.setMixInAnnotations(MediaStream.class, MediaStreamMixIn.class);
    context.setMixInAnnotations(Attributes.class, AttributesMixIn.class);
  }

  static class StreamPrintMixIn extends StreamPrint {
    @JsonCreator
    public StreamPrintMixIn(StringURI uri, Long size, long lastModificationTime, byte[] hash, Long openSubtitleHash) {
      super(uri, size, lastModificationTime, hash, openSubtitleHash);
    }
  }

  static class AttributesMixIn extends Attributes {
    @JsonCreator
    public AttributesMixIn(Map<String, Object> attributes) {
      super(attributes);
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
  static class MediaStreamMixIn {
  }
}
