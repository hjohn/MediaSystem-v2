package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.scanner.api.StreamID;

public interface StreamMetaDataStore {
  StreamMetaData find(StreamID streamId);
  byte[] readSnapshot(StreamID streamId, int snapshotIndex);
}
