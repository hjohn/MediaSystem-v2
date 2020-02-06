package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
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

  /**
   * Returns {@link MediaIdentification}, never null.  If there were no errors but the streamable
   * wasn't identifiable, an UnknownStreamableException is thrown.<p>
   *
   * This call may block to do I/O.
   *
   * @param streamable a {@link Streamable} to identify, cannot be null
   * @param parent a parent {@link MediaDescriptor}, if applicable, can be null
   * @param dataSourceName a data source name, cannot be null
   * @throws UnknownStreamableException when given streamable could not be identified
   * @throws UnknownDataSourceException when the given data source name is not known
   * @return a {@link MediaIdentification}, never null
   */
  public MediaIdentification identify(Streamable streamable, MediaDescriptor parent, String dataSourceName) {
    MediaType type = streamable.getType();
    DataSource dataSource = DataSource.instance(type, dataSourceName);
    IdentificationService service = identificationServicesByDataSource.get(dataSource);

    if(service == null) {
      throw new UnknownDataSourceException(dataSource);
    }

    return performIdentificationCall(streamable, parent, service);
  }

  private MediaIdentification performIdentificationCall(Streamable streamable, MediaDescriptor parent, IdentificationService service) {
    Identification identification = service.identify(streamable, parent).orElseThrow(() -> new UnknownStreamableException(streamable, service));

    return new MediaIdentification(streamable, identification, parent == null ? query(identification.getPrimaryIdentifier()) : null);
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
