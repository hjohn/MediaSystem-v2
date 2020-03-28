package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

public class Streamables {
  public static Streamable create() {
    return create("file://some-file", new ContentID(77));
  }

  public static Streamable create(String uri, ContentID contentId) {
    return create(MediaType.of("MOVIE"), uri, contentId, null);
  }

  public static Streamable create(MediaType type, String uri, ContentID contentId, String sequence) {
    return new Streamable(type, new StringURI(uri), contentId, null, Attributes.of(Attribute.TITLE, "Terminator", Attribute.SEQUENCE, sequence));
  }
}
