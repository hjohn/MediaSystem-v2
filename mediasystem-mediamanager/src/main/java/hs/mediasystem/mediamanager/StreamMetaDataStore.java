package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.StreamMetaData;

import java.util.Optional;

public interface StreamMetaDataStore {
  Optional<StreamMetaData> find(ContentID contentId);
  byte[] readSnapshot(ContentID contentId, int snapshotIndex);
}
