package hs.mediasystem.db.base;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
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

  private final Map<WorkId, CachedDescriptor> cache = new HashMap<>();

  // Indices
  private final Map<WorkId, WorkDescriptor> descriptors = new HashMap<>();

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

  synchronized void add(WorkDescriptor descriptor) {
    CachedDescriptor cachedDescriptor = new CachedDescriptor(Instant.now(), descriptor);

    database.store(codec.toRecord(cachedDescriptor));

    removeFromCache(cachedDescriptor);
    addToCache(cachedDescriptor);
  }

  private void addToCache(CachedDescriptor cd) {
    WorkId id = cd.getDescriptor().getId();

    cache.put(id, cd);
    descriptors.put(id, cd.getDescriptor());

    if(cd.getDescriptor() instanceof Serie serie) {
      serie.getSeasons().stream()
        .map(Season::getEpisodes)
        .flatMap(Collection::stream)
        .forEach(ep -> descriptors.put(ep.getId(), ep));
    }
  }

  private void removeFromCache(CachedDescriptor cd) {
    WorkId id = cd.getDescriptor().getId();

    if(cache.remove(id) != null) {
      descriptors.remove(id);

      if(cd.getDescriptor() instanceof Serie serie) {
        serie.getSeasons().stream()
          .map(Season::getEpisodes)
          .flatMap(Collection::stream)
          .forEach(ep -> descriptors.remove(ep.getId()));
      }
    }
  }

  @Override
  public synchronized Optional<WorkDescriptor> find(WorkId id) {
    return Optional.ofNullable(descriptors.get(id));
  }
}
