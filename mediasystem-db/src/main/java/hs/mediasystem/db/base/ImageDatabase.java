package hs.mediasystem.db.base;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageDatabase {
  private static final Set<String> ACCESS_TIME_SET = Set.of("accesstime");

  @Inject private Database database;

  public byte[] findByURI(URI uri) {
    return findImageByURI(uri).map(ImageRecord::getImage).orElse(null);
  }

  public Optional<ImageRecord> findImageByURI(URI uri) {
    try(Transaction tx = database.beginTransaction()) {
      ImageRecord image = tx.selectUnique(ImageRecord.class, "url = ?", uri.toString());

      if(image == null) {
        return Optional.empty();
      }

      image.setAccessTime(LocalDateTime.now());

      tx.update(image, ACCESS_TIME_SET);
      tx.commit();

      return Optional.of(image);
    }
  }

  public void store(URI uri, String key, byte[] data) {
    try(Transaction tx = database.beginTransaction()) {
      String uriString = uri.toString();
      ImageRecord image = tx.selectUnique(ImageRecord.class, "url = ?", uriString);

      if(image == null) {
        tx.insert(new ImageRecord(uriString, key, data));
      }
      else {
        image.setCreationTime(LocalDateTime.now());
        image.setKey(key);
        image.setImage(data);

        tx.update(image);
      }

      tx.commit();
    }
  }
}
