package hs.mediasystem.db;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageStore {
  private static final Set<String> ACCESS_TIME_SET = Set.of("accesstime");

  @Inject private Database database;

  public byte[] findByURL(URL url) {
    return findImageByURL(url).map(Image::getImage).orElse(null);
  }

  public Optional<Image> findImageByURL(URL url) {
    try(Transaction tx = database.beginTransaction()) {
      Image image = tx.selectUnique(Image.class, "url = ?", url.toExternalForm());

      if(image == null) {
        return Optional.empty();
      }

      image.setAccessTime(LocalDateTime.now());

      tx.update(image, ACCESS_TIME_SET);
      tx.commit();

      return Optional.of(image);
    }
  }

  public void store(URL url, byte[] data) {
    try(Transaction tx = database.beginTransaction()) {
      Image image = tx.selectUnique(Image.class, "url = ?", url.toExternalForm());

      if(image == null) {
        tx.insert(new Image(url.toExternalForm(), data));
      }
      else {
        image.setCreationTime(LocalDateTime.now());
        image.setImage(data);

        tx.update(image);
      }

      tx.commit();
    }
  }
}
