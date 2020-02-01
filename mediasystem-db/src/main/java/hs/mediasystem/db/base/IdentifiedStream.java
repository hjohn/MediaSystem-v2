package hs.mediasystem.db.base;

import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IdentifiedStream {
  private final BasicStream stream;
  private final Map<Identifier, Match> matches;

  public IdentifiedStream(BasicStream stream, Map<Identifier, Match> matches) {
    this.stream = stream;
    this.matches = Collections.unmodifiableMap(new HashMap<>(matches));
  }

  public BasicStream getStream() {
    return stream;
  }

  public Map<Identifier, Match> getMatches() {
    return matches;
  }
}
