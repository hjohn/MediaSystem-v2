package hs.mediasystem.db.base;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseDescriptorStore implements DescriptorStore {
  private static final Logger LOGGER = Logger.getLogger(DatabaseDescriptorStore.class.getName());

  private final Map<Identifier, CachedDescriptor> cache = new HashMap<>();

  // Indices
  private final Map<Identifier, MediaDescriptor> descriptors = new HashMap<>();

  @Inject private DescriptorDatabase database;
  @Inject private CachedDescriptorCodec codec;

  @PostConstruct
  private void postConstruct() {
    List<String> badIds = new ArrayList<>();

    database.forEach(r -> {
      try {
        addToCache(codec.fromRecord(r));
      }
      catch(IOException e) {
        LOGGER.warning("Exception decoding record: " + r + ": " + Throwables.formatAsOneLine(e));

        badIds.add(r.getIdentifier());
      }
    });

    badIds.stream().forEach(database::delete);

    LOGGER.fine("Loaded " + cache.size() + " cached descriptor records, deleted " + badIds.size() + " bad ones");
  }

  synchronized void add(MediaDescriptor descriptor) {
    CachedDescriptor cachedDescriptor = new CachedDescriptor(Instant.now(), descriptor);

    database.store(codec.toRecord(cachedDescriptor));

    removeFromCache(cachedDescriptor);
    addToCache(cachedDescriptor);
  }

  private void addToCache(CachedDescriptor cd) {
    Identifier identifier = cd.getDescriptor().getIdentifier();

    cache.put(identifier, cd);
    descriptors.put(identifier, cd.getDescriptor());

    if(cd.getDescriptor() instanceof Serie) {
      Serie serie = (Serie)cd.getDescriptor();

      serie.getSeasons().stream()
        .map(Season::getEpisodes)
        .flatMap(Collection::stream)
        .forEach(ep -> descriptors.put(ep.getIdentifier(), ep));
    }
  }

  private void removeFromCache(CachedDescriptor cd) {
    Identifier identifier = cd.getDescriptor().getIdentifier();

    if(cache.remove(identifier) != null) {
      descriptors.remove(identifier);

      if(cd.getDescriptor() instanceof Serie) {
        Serie serie = (Serie)cd.getDescriptor();

        serie.getSeasons().stream()
          .map(Season::getEpisodes)
          .flatMap(Collection::stream)
          .forEach(ep -> descriptors.remove(ep.getIdentifier()));
      }
    }
  }

  @Override
  public synchronized Optional<MediaDescriptor> find(Identifier identifier) {
    return Optional.ofNullable(descriptors.get(identifier));
  }
}
