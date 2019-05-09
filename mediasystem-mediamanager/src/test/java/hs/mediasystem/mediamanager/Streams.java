package hs.mediasystem.mediamanager;

import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamPrint;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.util.Collections;
import java.util.List;

public class Streams {
  public static BasicStream create() {
    return create("file://some-file", StreamPrints.create());
  }

  public static BasicStream create(String uri, StreamPrint streamPrint) {
    return create(MediaType.of("MOVIE"), uri, streamPrint, null);
  }

  public static BasicStream create(String uri, StreamPrint streamPrint, List<BasicStream> childStreams) {
    return create(MediaType.of("MOVIE"), uri, streamPrint, null, childStreams);
  }

  public static BasicStream create(MediaType type, String uri, StreamPrint streamPrint, String sequence) {
    return create(type, uri, streamPrint, sequence, Collections.emptyList());
  }

  public static BasicStream create(MediaType type, String uri, StreamPrint streamPrint, String sequence, List<BasicStream> childStreams) {
    return new BasicStream(type, new StringURI(uri), streamPrint, Attributes.of(Attribute.TITLE, "Terminator", Attribute.SEQUENCE, sequence), childStreams);
  }
}
