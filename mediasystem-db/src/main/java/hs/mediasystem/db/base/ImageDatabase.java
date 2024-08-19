package hs.mediasystem.db.base;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.db.core.api.Database;
import org.int4.db.core.api.Transaction;
import org.int4.db.core.reflect.Reflector;

@Singleton
public class ImageDatabase {
  private static final Reflector<ImageRecord> ALL = Reflector.of(ImageRecord.class).withNames("url", "logical_key", "creationtime", "accesstime", "image");

  @Inject private Database database;

  public byte[] findByURI(URI uri) {
    return findImageByURI(uri).map(ImageRecord::image).orElse(null);
  }

  public Optional<ImageRecord> findImageByURI(URI uri) {
    try(Transaction tx = database.beginTransaction()) {
      String uriString = uri.toString();
      ImageRecord image = tx."SELECT \{ALL} FROM images WHERE url = \{uriString}"
        .map(ALL)
        .get();

      if(image == null) {
        return Optional.empty();
      }

      image = image.withAccessTime(LocalDateTime.now());

      tx."UPDATE images SET accesstime = \{image.accessTime()} WHERE url = \{uriString}".execute();
      tx.commit();

      return Optional.of(image);
    }
  }

  public void store(URI uri, String key, byte[] data) {
    try(Transaction tx = database.beginTransaction()) {
      String uriString = uri.toString();

      tx."""
        INSERT INTO images (\{ALL})
          VALUES (\{ImageRecord.of(uriString, key, data)})
          ON CONFLICT (url)
          DO UPDATE SET logical_key = EXCLUDED.logical_key, creationtime = EXCLUDED.creationtime, image = EXCLUDED.image
      """.execute();

      tx.commit();
    }
  }
}
