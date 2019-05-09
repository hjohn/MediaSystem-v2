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
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.StringURI;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RecordGroupModule extends SimpleModule {

  public RecordGroupModule() {
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
        return MediaType.of(p.getText());
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
      new NamedType(Movie.class, "Movie"),
      new NamedType(Serie.class, "Serie"),
      new NamedType(Episode.class, "Episode")
    );

    super.setupModule(context);

    context.setMixInAnnotations(Attributes.class, AttributesMixIn.class);
    context.setMixInAnnotations(MediaDescriptor.class, MediaDescriptorMixIn.class);
    context.setMixInAnnotations(ProductionCollection.class, ProductionCollectionMixin.class);
  }

  static class AttributesMixIn extends Attributes {
    @JsonCreator
    public AttributesMixIn(Map<String, Object> attributes) {
      super(attributes);
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
  static class MediaDescriptorMixIn {
  }

  static class ProductionCollectionMixin extends ProductionCollection {
    @JsonCreator
    protected ProductionCollectionMixin(boolean complete, Identifier identifier, String name, String overview, ImageURI image, ImageURI backdrop, List<Production> productions) {
      super(complete, identifier, name, overview, image, backdrop, productions);
    }
  }
}
