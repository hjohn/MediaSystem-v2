package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;

import java.util.Map;
import java.util.Set;

public interface BasicStreamStore {
  void putIdentifications(StreamID streamId, Map<Identifier, Identification> identifications);

  /*
   * Look up functions
   */

  BasicStream findStream(StreamID streamId);
  StreamSource findStreamSource(StreamID streamId);
  Map<Identifier, Identification> findIdentifications(StreamID streamId);
  Set<BasicStream> findStreams(Identifier identifier);
  Set<BasicStream> findStreams(MediaType type, String tag);
  Map<BasicStream, Map<Identifier, Identification>> findIdentifiersByStreams(MediaType type, String tag);
}
