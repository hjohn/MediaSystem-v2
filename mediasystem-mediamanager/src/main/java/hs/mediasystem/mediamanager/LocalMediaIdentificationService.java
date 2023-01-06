package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalMediaIdentificationService {
  private static final Logger LOGGER = Logger.getLogger(LocalMediaIdentificationService.class.getName());

  @Inject private List<QueryService> queryServices;

  private final Map<TypedDataSource, QueryService> queryServicesByDataSource = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    LOGGER.info("Instantiated with " + queryServices.size() + " query services");

    for(QueryService service : queryServices) {
      if(queryServicesByDataSource.put(new TypedDataSource(service.getDataSource(), service.getMediaType()), service) != null) {
        LOGGER.warning("Multiple query services available for datasource: " + service.getDataSource());
      }
    }
  }

  public WorkDescriptor query(WorkId id) throws IOException {
    TypedDataSource dataSource = new TypedDataSource(id.getDataSource(), id.getType());
    QueryService queryService = queryServicesByDataSource.get(dataSource);

    if(queryService == null) {
      throw new UnknownDataSourceException(dataSource);
    }

    return queryService.query(id);
  }

  public boolean isQueryServiceAvailable(DataSource dataSource, MediaType mediaType) {
    TypedDataSource typeDataSource = new TypedDataSource(dataSource, mediaType);

    return queryServicesByDataSource.containsKey(typeDataSource);
  }
}
