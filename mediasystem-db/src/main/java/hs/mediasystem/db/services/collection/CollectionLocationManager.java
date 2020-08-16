package hs.mediasystem.db.services.collection;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.db.base.ImportSource;
import hs.mediasystem.db.base.ImportSourceProvider;
import hs.mediasystem.domain.work.CollectionDefinition;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Scanner;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamTags;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class CollectionLocationManager {
  private static final Logger LOGGER = Logger.getLogger(CollectionLocationManager.class.getName());
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory())
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    .registerModule(new ParameterNamesModule(Mode.PROPERTIES));

  @Inject private List<Scanner> scanners;
  @Inject private ImportSourceProvider importSourceProvider;
  @Inject @Nullable @Named("general.basedir") private String baseDir = ".";

  private List<CollectionDefinition> collectionDefinitions = new ArrayList<>();
  private List<ImportDefinition> importDefinitions = new ArrayList<>();

  @PostConstruct
  public void postConstruct() {
    reinstall();
  }

  public void reinstall() {
    File file = new File(baseDir, "mediasystem-collections.yaml");

    try {
      collectionDefinitions = OBJECT_MAPPER.readValue(file, new TypeReference<List<CollectionDefinition>>() {});
    }
    catch(IOException e) {
      throw new IllegalStateException("Error parsing " + file, e);
    }

    LOGGER.info("Loaded " + file);

    file = new File(baseDir, "mediasystem-imports.yaml");

    try {
      importDefinitions = OBJECT_MAPPER.readValue(file, new TypeReference<List<ImportDefinition>>() {});
    }
    catch(IOException e) {
      throw new IllegalStateException("Error parsing " + file, e);
    }

    LOGGER.info("Loaded " + file);

    List<ImportSource> sources = new ArrayList<>();

    for(ImportDefinition definition : importDefinitions) {
      String type = definition.getType() + "Scanner";

      for(Scanner scanner : scanners) {
        if(scanner.getClass().getSimpleName().equals(type)) {
          List<Path> paths = definition.getPaths().stream()
            .map(Paths::get)
            .collect(Collectors.toList());

          for(int i = 0; i < paths.size(); i++) {
            sources.add(new ImportSource(
              scanner,
              definition.getId() + i * 65536, paths.get(i),
              new StreamSource(new StreamTags(definition.getTags()), definition.getIdentification() == null ? Collections.emptyList() : List.of(definition.getIdentification()))
            ));
          }
        }
      }
    }

    importSourceProvider.set(sources);
  }

  public List<CollectionDefinition> getCollectionDefinitions(String type) {
    return collectionDefinitions.stream()
      .filter(cd -> cd.getType().equals(type))
      .collect(Collectors.toList());
  }

  public List<CollectionDefinition> getCollectionDefinitions() {
    return List.copyOf(collectionDefinitions);
  }

  public static class ImportDefinition {
    private final String type;
    private final int id;
    private final List<String> paths;
    private final Set<String> tags;
    private final String identification;

    public ImportDefinition(String type, int id, Set<String> tags, List<String> paths, String identification) {
      this.type = type;
      this.id = id;
      this.tags = Collections.unmodifiableSet(new HashSet<>(tags));
      this.paths = Collections.unmodifiableList(new ArrayList<>(paths));
      this.identification = identification;
    }

    public int getId() {
      return id;
    }

    public List<String> getPaths() {
      return paths;
    }

    public Set<String> getTags() {
      return tags;
    }

    public String getType() {
      return type;
    }

    public String getIdentification() {
      return identification;
    }
  }
}