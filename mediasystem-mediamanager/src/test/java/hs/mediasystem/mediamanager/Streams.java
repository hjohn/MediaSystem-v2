package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.Attribute;
import hs.mediasystem.domain.stream.BasicStream;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.util.Collections;
import java.util.List;

public class Streams {
  public static BasicStream create() {
    return create("file://some-file", new StreamID(77));
  }

  public static BasicStream create(String uri, StreamID streamId) {
    return create(MediaType.of("MOVIE"), uri, streamId, null);
  }

  public static BasicStream create(String uri, StreamID streamId, List<BasicStream> childStreams) {
    return create(MediaType.of("MOVIE"), uri, streamId, null, childStreams);
  }

  public static BasicStream create(MediaType type, String uri, StreamID streamId, String sequence) {
    return create(type, uri, streamId, sequence, Collections.emptyList());
  }

  public static BasicStream create(MediaType type, String uri, StreamID streamId, String sequence, List<BasicStream> childStreams) {
    return new BasicStream(type, new StringURI(uri), streamId, Attributes.of(Attribute.TITLE, "Terminator", Attribute.SEQUENCE, sequence), childStreams);
  }
}
