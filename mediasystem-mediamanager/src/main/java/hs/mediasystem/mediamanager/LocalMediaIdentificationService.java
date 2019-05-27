package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService.Result;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;
import hs.mediasystem.util.Tuple.Tuple3;

import java.util.ArrayList;
import java.util.Collection;
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
  @Inject private List<QueryService<?>> queryServices;

  private final Map<DataSource, IdentificationService> identificationServicesByDataSource = new HashMap<>();
  private final Map<DataSource, QueryService<MediaDescriptor>> queryServicesByDataSource = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    LOGGER.info("Instantiated with " + identificationServices.size() + " identification services and " + queryServices.size() + " query services");

    for(IdentificationService service : identificationServices) {
      if(identificationServicesByDataSource.put(service.getDataSource(), service) != null) {
        LOGGER.warning("Multiple identification services available for datasource: " + service.getDataSource());
      }
    }

    for(QueryService<?> service : queryServices) {
      @SuppressWarnings("unchecked")
      QueryService<MediaDescriptor> castedService = (QueryService<MediaDescriptor>)service;

      if(queryServicesByDataSource.put(service.getDataSource(), castedService) != null) {
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

    Set<DataSource> identifiedDataSources = new HashSet<>();
    List<Exceptional<Tuple3<Identifier, Identification, Result<MediaDescriptor>>>> results = new ArrayList<>();

    // Loop here as queries may return new Identifications that in turn require querying:
    while(!identificationsToQuery.isEmpty()) {
      identificationsToQuery
        .forEach(identificationExceptional -> results.add(
          identificationExceptional
            .map((Tuple2<Identifier, Identification> t) -> queryServicesByDataSource.get(t.a.getDataSource()))
            .map(qs -> qs.query(identificationExceptional.get().a))  // get() is safe here, won't get here if not present
            .map(r -> Tuple.of(identificationExceptional.get().a, identificationExceptional.get().b, r))
            .or(() -> Exceptional.of(Tuple.of(identificationExceptional.get().a, identificationExceptional.get().b, null)))
      ));

      // Update identifiedDataSources:
      identificationsToQuery.forEach(exI -> exI.map(t -> t.a)
        .map(Identifier::getDataSource)
        .ignore(Throwable.class)
        .ifPresent(identifiedDataSources::add)
      );

      // Check Results for new Identifications:
      identificationsToQuery = results.stream()
        .map(e -> e.map(t -> t.c))
        .flatMap(Exceptional::ignoreAllAndStream)
        .map(Result::getNewIdentifications)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .map(e -> Tuple.of(e.getKey(), e.getValue()))
        .filter(t -> !identifiedDataSources.contains(t.a.getDataSource()))  // Don't identify ones that exist already (prevent id loops)
        .map(Exceptional::of)
        .collect(Collectors.toList());
    }

    return results.stream()
      .map(e -> e.map(t -> Tuple.of(t.a, t.b, t.c == null ? null : t.c.getMediaDescriptor())))
      .collect(Collectors.toSet());
  }
}
