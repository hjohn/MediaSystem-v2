package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;
import hs.mediasystem.util.Tuple.Tuple3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
  public MediaIdentification identify(BasicStream stream, List<String> allowedDataSourceNames) {
    MediaType type = stream.getType();

    // Create list of identifications to perform:
    List<IdentificationService> identificationsToPerform = identificationServicesByDataSource.entrySet().stream()
      .filter(e -> e.getKey().getType().equals(type))
      .filter(e -> allowedDataSourceNames.contains(e.getKey().getName()))  // Must match data source specified by StreamSource
      .map(Map.Entry::getValue)
      .collect(Collectors.toList());

    return new MediaIdentification(
      stream,
      performIdentificationCalls(stream, identificationsToPerform)
    );
  }

  private Set<Exceptional<Tuple3<Identifier, Identification, MediaDescriptor>>> performIdentificationCalls(BasicStream stream, List<IdentificationService> identificationsToPerform) {
    // Get new identifications:
    List<Exceptional<Tuple2<Identifier, Identification>>> identificationsToQuery = identificationsToPerform.stream()
      .map(identificationService -> Exceptional.from(() ->
        Optional.ofNullable(
          identificationService.identify(stream)
        ).orElseThrow(() -> new UnknownStreamException(stream, identificationService))
      ))
      .collect(Collectors.toList());

    Set<Exceptional<Tuple3<Identifier, Identification, MediaDescriptor>>> results = new HashSet<>();

    identificationsToQuery
      .forEach(identificationExceptional -> results.add(
        identificationExceptional
          .map((Tuple2<Identifier, Identification> t) -> queryServicesByDataSource.get(t.a.getDataSource()))
          .map(qs -> qs.query(identificationExceptional.get().a))  // get() is safe here, won't get here if not present
          .map(r -> Tuple.of(identificationExceptional.get().a, identificationExceptional.get().b, r))
          .or(() -> Exceptional.of(Tuple.of(identificationExceptional.get().a, identificationExceptional.get().b, null)))
    ));

    return results;
  }

  public Exceptional<MediaDescriptor> query(Identifier identifier) {
    return Exceptional.ofNullable(queryServicesByDataSource.get(identifier.getDataSource()))
      .map(qs -> qs.query(identifier));
  }

  public boolean isQueryServiceAvailable(DataSource dataSource) {
    return queryServicesByDataSource.containsKey(dataSource);
  }
}
