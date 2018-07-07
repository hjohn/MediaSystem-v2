package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.util.Attributes;

import java.util.Map;

public class MovieStream extends AbstractMediaStream<MovieDescriptor> {

  public MovieStream(StreamPrint streamPrint, Attributes attributes, Map<Identifier, MediaRecord<MovieDescriptor>> mediaRecords) {
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
