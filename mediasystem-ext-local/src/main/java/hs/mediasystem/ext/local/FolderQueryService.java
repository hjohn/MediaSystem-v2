package hs.mediasystem.ext.local;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Folder;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.mediamanager.StreamableStore;
import hs.mediasystem.util.Attributes;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FolderQueryService extends AbstractQueryService {
  private static final DataSource FOLDER = DataSource.instance(MediaType.of("FOLDER"), "LOCAL");

  @Inject private StreamableStore streamStore;
  @Inject private DescriptionService descriptionService;

  public FolderQueryService() {
    super(FOLDER);
  }

  @Override
  public Folder query(Identifier identifier) {
    StreamID streamId = StreamID.of(identifier.getId());
    Streamable streamable = streamStore.findStream(streamId).orElseThrow();
    Optional<Description> description = descriptionService.loadDescription(streamable);
    Attributes attributes = streamable.getAttributes();

    return new Folder(
      (ProductionIdentifier)identifier,
      new Details(
        description.map(Description::getTitle).orElse(attributes.get(Attribute.TITLE)),
        description.map(Description::getSubtitle).orElse(attributes.get(Attribute.SUBTITLE)),
        description.map(Description::getDescription).orElse(attributes.get(Attribute.DESCRIPTION)),
        description.map(Description::getDate).orElse(null),
        description.map(Description::getCover).orElse(null),
        description.map(Description::getBackdrop).orElse(null)
      ),
      null,
      List.of(),
      description.map(Description::getGenres).orElse(List.of())
    );
  }
}
