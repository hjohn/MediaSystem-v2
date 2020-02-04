package hs.mediasystem.ext.local;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;
import hs.mediasystem.ext.basicmediatypes.services.AbstractIdentificationService;

import java.time.Instant;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class LocalIdentificationService extends AbstractIdentificationService {
  private static final DataSource SERIE = DataSource.instance(MediaType.of("SERIE"), "LOCAL");

  public LocalIdentificationService() {
    super(SERIE);
  }

  @Override
  public Map<StreamID, Identification> identify(BasicStream stream) {
    return Map.of(
      stream.getId(),
      new Identification(new ProductionIdentifier(SERIE, "" + stream.getId().asInt()), new Match(MatchType.MANUAL, 1.0f, Instant.now()))
    );
  }
}
