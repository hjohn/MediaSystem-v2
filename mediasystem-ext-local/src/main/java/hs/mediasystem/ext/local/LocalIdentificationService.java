package hs.mediasystem.ext.local;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.domain.Classification;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Folder;
import hs.mediasystem.api.datasource.domain.Production;
import hs.mediasystem.api.datasource.services.IdentificationService;
import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.Attributes;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalIdentificationService implements IdentificationService {
  private static final DataSource LOCAL = DataSource.instance("LOCAL");

  @Inject private DescriptionService descriptionService;

  @Override
  public String getName() {
    return "LOCAL";
  }

  @Override
  public Optional<Identification> identify(Discovery discovery, WorkDescriptor parent) {
    MediaType mediaType = discovery.mediaType();

    if(mediaType == MediaType.FOLDER) {
      return Optional.of(new Identification(List.of(queryFolder(discovery)), new Match(Type.MANUAL, 1.0f, Instant.now())));
    }
    if(mediaType == MediaType.FILE) {
      return Optional.of(new Identification(List.of(queryFile(discovery)), new Match(Type.MANUAL, 1.0f, Instant.now())));
    }

    throw new IllegalArgumentException("Unsupported media type: " + mediaType);
  }

  private Folder queryFolder(Discovery discovery) {
    URI location = discovery.location();
    Optional<Description> description = descriptionService.loadDescription(location);
    Attributes attributes = discovery.attributes();

    return new Folder(
      new WorkId(LOCAL, MediaType.FOLDER, location.toString()),
      new Details(
        description.map(Description::getTitle).orElse(attributes.get(Attribute.TITLE)),
        description.map(Description::getSubtitle).orElse(attributes.get(Attribute.SUBTITLE)),
        description.map(Description::getDescription).orElse(attributes.get(Attribute.DESCRIPTION)),
        description.map(Description::getDate).orElse(null),
        descriptionService.getCover(location).orElse(null),
        null,
        descriptionService.getBackdrop(location).orElse(null)
      ),
      null,
      new Classification(
        description.map(Description::getGenres).orElse(List.of()),
        List.of(),
        List.of(),
        Map.of(),
        null
      )
    );
  }

  private static Production queryFile(Discovery discovery) {
    URI location = discovery.location();
    Attributes attributes = discovery.attributes();

    return new Production(
      new WorkId(LOCAL, MediaType.FILE, location.toString()),
      new Details(
        attributes.get(Attribute.TITLE),
        attributes.get(Attribute.SUBTITLE),
        attributes.get(Attribute.ALTERNATIVE_TITLE),
        null,
        null,
        null,
        null
      ),
      null,
      null,
      null,
      Classification.EMPTY,
      1.0
    );
  }
}
