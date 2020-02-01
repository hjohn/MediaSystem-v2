package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;

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

  @Inject private List<IdentificationService> identificationServices;
  @Inject private List<QueryService> queryServices;

  private final Map<DataSource, IdentificationService> identificationServicesByDataSource = new HashMap<>();
  private final Map<DataSource, QueryService> queryServicesByDataSource = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    LOGGER.info("Instantiated with " + identificationServices.size() + " identification services and " + queryServices.size() + " query services");

    for(IdentificationService service : identificationServices) {
      if(identificationServicesByDataSource.put(service.getDataSource(), service) != null) {
        LOGGER.warning("Multiple identification services available for datasource: " + service.getDataSource());
      }
    }

    for(QueryService service : queryServices) {
      if(queryServicesByDataSource.put(service.getDataSource(), service) != null) {
        LOGGER.warning("Multiple query services available for datasource: " + service.getDataSource());
      }
    }
  }

  // Method may block
  public MediaIdentification identify(BasicStream stream, String dataSourceName) {
    MediaType type = stream.getType();
    DataSource dataSource = DataSource.instance(type, dataSourceName);
    IdentificationService service = identificationServicesByDataSource.get(dataSource);

    if(service == null) {
      throw new UnknownDataSourceException(dataSource);
    }

    return performIdentificationCall(stream, service);
  }

  private MediaIdentification performIdentificationCall(BasicStream stream, IdentificationService service) {
    Map<StreamID, Identification> result = service.identify(stream);

    if(result.isEmpty()) {
      return new MediaIdentification(stream, result, null);
    }

    return new MediaIdentification(stream, result, query(result.get(stream.getId()).getIdentifier()));
  }

  public MediaDescriptor query(Identifier identifier) {
    QueryService queryService = queryServicesByDataSource.get(identifier.getDataSource());

    if(queryService == null) {
      throw new UnknownDataSourceException(identifier.getDataSource());
    }

    return queryService.query(identifier);
  }

  public boolean isQueryServiceAvailable(DataSource dataSource) {
    return queryServicesByDataSource.containsKey(dataSource);
  }
}
