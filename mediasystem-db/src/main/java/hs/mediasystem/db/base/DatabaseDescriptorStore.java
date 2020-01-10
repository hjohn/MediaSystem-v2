package hs.mediasystem.db.base;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
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

  @Inject private DescriptorDatabase database;
  @Inject private CachedDescriptorCodec codec;

  @PostConstruct
  private void postConstruct() {
    List<String> badIds = new ArrayList<>();

    database.forEach(r -> {
      try {
        CachedDescriptor cd = codec.fromRecord(r);

        cache.put(cd.getDescriptor().getIdentifier(), cd);
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

    cache.put(descriptor.getIdentifier(), cachedDescriptor);
  }

  @Override
  public synchronized Optional<MediaDescriptor> find(Identifier identifier) {
    CachedDescriptor cd = cache.get(identifier);

    return cd == null ? Optional.empty() : Optional.of(cd.getDescriptor());
  }
}
