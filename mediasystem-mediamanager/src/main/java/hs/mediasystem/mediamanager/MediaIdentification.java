package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.Tuple.Tuple3;

import java.util.Set;

public class MediaIdentification {
  private final Set<Exceptional<Tuple3<Identifier, Match, MediaDescriptor>>> results;
  private final BasicStream media;

  public MediaIdentification(BasicStream media, Set<Exceptional<Tuple3<Identifier, Match, MediaDescriptor>>> set) {
    this.media = media;
    this.results = set;
  }

  public Set<Exceptional<Tuple3<Identifier, Match, MediaDescriptor>>> getResults() {
    return results;
  }

  public BasicStream getStream() {
    return media;
  }
}
