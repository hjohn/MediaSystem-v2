package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.BasicStream;
import hs.mediasystem.domain.work.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IdentifiedStream {
  private final BasicStream stream;
  private final Map<Identifier, Identification> identifications;

  public IdentifiedStream(BasicStream stream, Map<Identifier, Identification> identifications) {
    this.stream = stream;
    this.identifications = Collections.unmodifiableMap(new HashMap<>(identifications));
  }

  public BasicStream getStream() {
    return stream;
  }

  public Map<Identifier, Identification> getIdentifications() {
    return identifications;
  }
}
