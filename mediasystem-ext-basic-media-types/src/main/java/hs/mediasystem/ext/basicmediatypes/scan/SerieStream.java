package hs.mediasystem.ext.basicmediatypes.scan;

import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.Type;
import hs.mediasystem.util.Attributes;

import java.util.Map;

public class SerieStream extends AbstractMediaStream<Serie> {

  public SerieStream(StreamPrint streamPrint, Attributes attributes, Map<Identifier, MediaRecord<Serie>> mediaRecords) {
    super(Type.of("SERIE"), null, streamPrint, attributes, mediaRecords);

    if(!attributes.contains(Attribute.TITLE)) {
      throw new IllegalArgumentException("attributes must contain Attribute.TITLE");
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getStreamPrint().getUri().toURI().getPath() + "]";
  }
}
