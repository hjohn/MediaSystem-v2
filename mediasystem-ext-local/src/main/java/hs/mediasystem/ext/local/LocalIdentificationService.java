package hs.mediasystem.ext.local;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Identification;
import hs.mediasystem.domain.work.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;
import hs.mediasystem.ext.basicmediatypes.services.AbstractIdentificationService;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;

import java.time.Instant;

import javax.inject.Singleton;

@Singleton
public class LocalIdentificationService extends AbstractIdentificationService {
  private static final DataSource SERIE = DataSource.instance(MediaType.of("SERIE"), "LOCAL");

  public LocalIdentificationService() {
    super(SERIE);
  }

  @Override
  public Tuple2<Identifier, Identification> identify(BasicStream stream) {
    return Tuple.of(new ProductionIdentifier(SERIE, "" + stream.getId().asInt()), new Identification(MatchType.MANUAL, 1.0, Instant.now()));
  }
}
