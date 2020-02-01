package hs.mediasystem.db.base;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;

import java.util.Optional;

public class IdentifiedStream {
  private final BasicStream stream;
  private final Optional<Identification> identification;

  public IdentifiedStream(BasicStream stream, Identification identification) {
    this.stream = stream;
    this.identification = Optional.ofNullable(identification);
  }

  public BasicStream getStream() {
    return stream;
  }

  public Optional<Identification> getIdentification() {
    return identification;
  }
}
