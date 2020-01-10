package hs.mediasystem.db.services;

import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.mediamanager.StreamMetaDataStore;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageService {
  @Inject private StreamMetaDataStore store;

  public Optional<byte[]> findImage(String id) {
    String[] parts = id.split(":");

    return Optional.ofNullable(store.readSnapshot(new StreamID(Integer.parseInt(parts[0])), Integer.parseInt(parts[1])));
  }
}
