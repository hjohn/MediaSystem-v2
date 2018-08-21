package hs.mediasystem.ext.basicmediatypes.scan;

import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Type;
import hs.mediasystem.util.Attributes;

import java.util.Map;

public class MovieStream extends AbstractMediaStream<Movie> {

  public MovieStream(StreamPrint streamPrint, Attributes attributes, Map<Identifier, MediaRecord<Movie>> mediaRecords) {
    super(Type.of("MOVIE"), null, streamPrint, attributes, mediaRecords);

    if(!attributes.contains(Attribute.TITLE)) {
      throw new IllegalArgumentException("attributes must contain Attribute.TITLE");
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getStreamPrint().getUri().toURI().getPath() + "]";
  }
}
