package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.ContentID;

public interface StreamMetaDataStore {
  byte[] readSnapshot(ContentID contentId, int snapshotIndex);
}
