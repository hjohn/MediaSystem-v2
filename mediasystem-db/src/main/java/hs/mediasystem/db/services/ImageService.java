package hs.mediasystem.db.services;

import hs.mediasystem.db.extract.StreamDescriptorStore;
import hs.mediasystem.domain.stream.ContentID;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageService {
  @Inject private StreamDescriptorStore store;

  public Optional<byte[]> findImage(String id) throws IOException {
    String[] parts = id.split(":");

    try {
      return Optional.ofNullable(store.readSnapshot(new ContentID(Integer.parseInt(parts[0])), Integer.parseInt(parts[1])));
    }
    catch(SQLException e) {
      throw new IOException("Unable to read snapshot from database: " + id, e);
    }
  }
}
