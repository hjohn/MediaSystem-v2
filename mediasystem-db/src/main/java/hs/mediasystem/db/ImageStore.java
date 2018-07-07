package hs.mediasystem.db;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageStore {
  @Inject private Database database;

  public byte[] findByURL(URL url) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      Image image = tx.selectUnique(Image.class, "url = ?", url.toExternalForm());

      if(image == null) {
        return null;
      }

//      image.setAccessTime(LocalDateTime.now());  // TODO this saves the entire image again... only want a field update
//
//      tx.commit();

      return image.getImage();
    }
  }

  public void store(URL url, byte[] data) {
    try(Transaction tx = database.beginTransaction()) {
      Image image = tx.selectUnique(Image.class, "url = ?", url.toExternalForm());

      if(image == null) {
        tx.insert(new Image(url.toExternalForm(), data));
      }
      else {
        image.setImage(data);

        tx.update(image);
      }

      tx.commit();
    }
  }
}
