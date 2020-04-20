package hs.mediasystem.ext.local;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.mediamanager.StreamableStore;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FileQueryService extends AbstractQueryService {
  private static final DataSource FILE = DataSource.instance(MediaType.of("FILE"), "LOCAL");

  @Inject private StreamableStore streamStore;

  public FileQueryService() {
    super(FILE);
  }

  @Override
  public Production query(Identifier identifier) {
    StreamID streamId = StreamID.of(identifier.getId());
    Streamable streamable = streamStore.findStream(streamId).orElseThrow();

    return new Production(
      (ProductionIdentifier)identifier,
      new Details(
        streamable.getAttributes().get(Attribute.TITLE),
        streamable.getAttributes().get(Attribute.SUBTITLE),
        streamable.getAttributes().get(Attribute.ALTERNATIVE_TITLE),
        null,
        null,
        null
      ),
      null,
      List.of(),
      List.of(),
      1.0,
      Set.of()
    );
  }
}
