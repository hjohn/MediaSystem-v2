package hs.mediasystem.ext.local;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Folder;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.mediamanager.StreamableStore;
import hs.mediasystem.util.Attributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FolderQueryService extends AbstractQueryService {
  private static final DataSource LOCAL = DataSource.instance("LOCAL");

  @Inject private StreamableStore streamStore;
  @Inject private DescriptionService descriptionService;

  public FolderQueryService() {
    super(LOCAL, MediaType.FOLDER);
  }

  @Override
  public Folder query(WorkId id) {
    StreamID streamId = StreamID.of(id.getKey());
    Streamable streamable = streamStore.findStream(streamId).orElseThrow();
    Optional<Description> description = descriptionService.loadDescription(streamable);
    Attributes attributes = streamable.getAttributes();

    return new Folder(
      id,
      new Details(
        description.map(Description::getTitle).orElse(attributes.get(Attribute.TITLE)),
        description.map(Description::getSubtitle).orElse(attributes.get(Attribute.SUBTITLE)),
        description.map(Description::getDescription).orElse(attributes.get(Attribute.DESCRIPTION)),
        description.map(Description::getDate).orElse(null),
        descriptionService.getCover(streamable).orElse(null),
        null,
        descriptionService.getBackdrop(streamable).orElse(null)
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
}
