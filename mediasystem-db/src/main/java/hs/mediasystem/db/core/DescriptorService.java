package hs.mediasystem.db.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.services.QueryService;
import hs.mediasystem.db.DatabaseResponseCache;
import hs.mediasystem.db.DatabaseResponseCache.CacheMode;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;

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

  private final Map<DataSource, QueryService> queryServicesByDataSource = new HashMap<>();
  private final Cache<WorkId, WorkDescriptor> cache = Caffeine.newBuilder()
    .maximumSize(10000)
    .build();

  @PostConstruct
  private void postConstruct() {
    for(QueryService service : queryServices) {
      if(queryServicesByDataSource.put(service.getDataSource(), service) != null) {
        LOGGER.warning("Multiple query services available for datasource: " + service.getDataSource());
      }
    }
  }

  public Optional<? extends WorkDescriptor> find(WorkId id) throws IOException {
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

  private Optional<? extends WorkDescriptor> queryWork(WorkId id) throws IOException {
    QueryService queryService = queryServicesByDataSource.get(id.getDataSource());

    if(queryService == null) {
      return Optional.empty();
    }

    return queryService.query(id).map(d -> {
      cache.put(id, d);  // Bit ugly...

      return d;
    });
  }
}
