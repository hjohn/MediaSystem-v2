package hs.mediasystem.ext.local;

import hs.ddif.annotations.PluginScoped;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.mediamanager.StreamableStore;

import java.util.Set;

import javax.inject.Inject;

@PluginScoped
public class FileQueryService extends AbstractQueryService {
  private static final DataSource FILE = DataSource.instance(MediaType.FILE, "LOCAL");

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
      Classification.EMPTY,
      1.0,
      Set.of()
    );
  }
}
