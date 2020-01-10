package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.StreamMetaData;

import java.util.Optional;

public interface StreamMetaDataStore {
  Optional<StreamMetaData> find(StreamID streamId);
  byte[] readSnapshot(StreamID streamId, int snapshotIndex);
}
