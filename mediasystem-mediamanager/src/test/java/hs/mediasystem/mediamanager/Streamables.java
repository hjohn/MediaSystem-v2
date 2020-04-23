package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

public class Streamables {
  public static Streamable create() {
    return create("file://some-file", new StreamID(1, new ContentID(77), "Terminator"));
  }

  public static Streamable create(String uri, StreamID streamId) {
    return create(MediaType.MOVIE, uri, streamId, null);
  }

  public static Streamable create(MediaType type, String uri, StreamID streamId, String sequence) {
    return new Streamable(type, new StringURI(uri), streamId, null, Attributes.of(Attribute.TITLE, "Terminator", Attribute.SEQUENCE, sequence));
  }
}
