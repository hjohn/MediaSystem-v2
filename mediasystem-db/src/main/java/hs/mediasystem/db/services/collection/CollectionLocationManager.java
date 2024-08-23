package hs.mediasystem.db.services.collection;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.api.discovery.Discoverer;
import hs.mediasystem.db.core.ImportSource;
import hs.mediasystem.db.core.domain.StreamTags;
import hs.mediasystem.domain.work.CollectionDefinition;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.int4.dirk.annotations.Produces;

@Singleton
public class CollectionLocationManager {
  private static final Logger LOGGER = Logger.getLogger(CollectionLocationManager.class.getName());
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory())
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    .registerModule(new ParameterNamesModule(Mode.PROPERTIES));

  @Inject private List<Discoverer> discoverers;
  @Inject @Nullable @Named("general.basedir") private String baseDir = ".";

  private List<CollectionDefinition> collectionDefinitions = new ArrayList<>();

  @PostConstruct
  void postConstruct() {
    loadConfiguration();
  }

  public List<CollectionDefinition> getCollectionDefinitions() {
    return List.copyOf(collectionDefinitions);
  }

  @Produces
  Collection<ImportSource> importSources() {
    File file = new File(baseDir, "mediasystem-imports.yaml");

    try {
      List<ImportDefinition> importDefinitions = OBJECT_MAPPER.readValue(file, new TypeReference<List<ImportDefinition>>() {});

      List<ImportSource> sources = new ArrayList<>();

      for(ImportDefinition definition : importDefinitions) {
        String type = definition.type() + "Discoverer";

        for(Discoverer discoverer : discoverers) {
          if(discoverer.getClass().getSimpleName().equals(type)) {
            Path root = Paths.get(definition.root());

            sources.add(new ImportSource(
              discoverer,
              root.toUri(),
              Optional.ofNullable(definition.identification),
              new StreamTags(definition.tags())
            ));
          }
        }
      }

      return Collections.unmodifiableCollection(sources);
    }
    catch(IOException e) {
      throw new IllegalStateException("Error parsing " + file, e);
    }
  }

  private void loadConfiguration() {
    File file = new File(baseDir, "mediasystem-collections.yaml");

    try {
      collectionDefinitions = OBJECT_MAPPER.readValue(file, new TypeReference<List<CollectionDefinition>>() {});
    }
    catch(IOException e) {
      throw new IllegalStateException("Error parsing " + file, e);
    }

    LOGGER.info("Loaded " + file);
  }

  private static record ImportDefinition(String root, String type, Set<String> tags, String identification) {}
}