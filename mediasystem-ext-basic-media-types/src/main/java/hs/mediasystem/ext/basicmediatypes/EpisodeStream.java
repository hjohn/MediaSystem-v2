package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.util.Map;

public class EpisodeStream extends AbstractMediaStream<EpisodeDescriptor> {

  public EpisodeStream(StringURI parentUri, StreamPrint streamPrint, Attributes attributes, Map<Identifier, MediaRecord<EpisodeDescriptor>> mediaRecords) {
    super(Type.of("EPISODE"), parentUri, streamPrint, attributes, mediaRecords);

    if(!attributes.contains(Attribute.TITLE)) {
      throw new IllegalArgumentException("attributes must contain Attribute.TITLE");
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getStreamPrint().getUri().toURI().getPath() + "]";
  }
}
