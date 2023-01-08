package hs.mediasystem.db.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import hs.mediasystem.db.DatabaseResponseCache;
import hs.mediasystem.db.DatabaseResponseCache.CacheMode;
import hs.mediasystem.db.DatabaseResponseCache.NotCachedException;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DescriptorService {
  private static final Logger LOGGER = Logger.getLogger(DescriptorService.class.getName());

  @Inject private List<QueryService> queryServices;
  @Inject private DatabaseResponseCache responseCache;

  private final Map<TypedDataSource, QueryService> queryServicesByDataSource = new HashMap<>();
  private final Cache<WorkId, WorkDescriptor> cache = Caffeine.newBuilder()
    .maximumSize(10000)
    .build();

  @PostConstruct
  private void postConstruct() {
    for(QueryService service : queryServices) {
      if(queryServicesByDataSource.put(new TypedDataSource(service.getDataSource(), service.getMediaType()), service) != null) {
        LOGGER.warning("Multiple query services available for datasource: " + service.getDataSource());
      }
    }
  }

  public Optional<WorkDescriptor> findCached(WorkId id) {
    WorkDescriptor descriptor = cache.getIfPresent(id);

    if(descriptor != null) {
      return Optional.of(descriptor);
    }

    CacheMode cacheMode = responseCache.getCurrentThreadCacheMode();

    responseCache.setCurrentThreadCacheMode(CacheMode.ONLY_CACHED);

    try {
      return queryWork(id);
    }
    catch(NotCachedException e) {
      LOGGER.warning("Work was not available in cache, skipped: " + id);
      // TODO perhaps a background task should be triggered to fetch this information so it is cached next time!
      return Optional.empty();
    }
    catch(IOException e) {
      throw new IllegalStateException("Unexpected exception as CacheMode was set to ONLY_CACHED while querying: " + id, e);
    }
    finally {
      responseCache.setCurrentThreadCacheMode(cacheMode);
    }
  }

  public Optional<WorkDescriptor> find(WorkId id) throws IOException {
    WorkDescriptor descriptor = cache.getIfPresent(id);

    if(descriptor != null) {
      return Optional.of(descriptor);
    }

    CacheMode cacheMode = responseCache.getCurrentThreadCacheMode();

    responseCache.setCurrentThreadCacheMode(CacheMode.PREFER_CACHED);

    try {
      return queryWork(id);
    }
    finally {
      responseCache.setCurrentThreadCacheMode(cacheMode);
    }
  }

  private Optional<WorkDescriptor> queryWork(WorkId id) throws IOException {
    TypedDataSource dataSource = new TypedDataSource(id.getDataSource(), id.getType());
    QueryService queryService = queryServicesByDataSource.get(dataSource);

    if(queryService == null) {
      return Optional.empty();
    }

    WorkDescriptor descriptor = queryService.query(id);

    cache.put(id, descriptor);

    return Optional.of(descriptor);
  }

  private static record TypedDataSource(DataSource dataSource, MediaType mediaType) {}
}
