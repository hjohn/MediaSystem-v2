package hs.mediasystem.db.services;

import hs.mediasystem.db.extract.DefaultStreamMetaDataStore;
import hs.mediasystem.domain.stream.ContentID;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageService {
  @Inject private DefaultStreamMetaDataStore store;

  public Optional<byte[]> findImage(String id) {
    String[] parts = id.split(":");

    return Optional.ofNullable(store.readSnapshot(new ContentID(Integer.parseInt(parts[0])), Integer.parseInt(parts[1]) - 1));
  }
}
