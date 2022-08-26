package hs.mediasystem.ext.local;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.mediamanager.StreamableStore;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FileQueryService extends AbstractQueryService {
  private static final DataSource LOCAL = DataSource.instance("LOCAL");

  @Inject private StreamableStore streamStore;

  public FileQueryService() {
    super(LOCAL, MediaType.FILE);
  }

  @Override
  public Production query(WorkId id) {
    StreamID streamId = StreamID.of(id.getKey());
    Streamable streamable = streamStore.findStream(streamId).orElseThrow();

    return new Production(
      id,
      new Details(
        streamable.getAttributes().get(Attribute.TITLE),
        streamable.getAttributes().get(Attribute.SUBTITLE),
        streamable.getAttributes().get(Attribute.ALTERNATIVE_TITLE),
        null,
        null,
        null,
        null
      ),
      null,
      null,
      Classification.EMPTY,
      1.0,
      Set.of()
    );
  }
}
