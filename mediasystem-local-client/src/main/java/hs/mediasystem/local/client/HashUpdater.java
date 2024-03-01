package hs.mediasystem.local.client;

import hs.mediasystem.db.contentprints.ContentPrintDatabase;
import hs.mediasystem.db.uris.UriDatabase;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.runner.config.BasicSetup;
import hs.mediasystem.util.MediaHash;
import hs.mediasystem.util.exception.Throwables;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.int4.dirk.api.Injector;

/**
 * Mini program to update all hash values in the database, by re-reading all
 * referenced files and setting their hash without changing their ContentID.
 */
public class HashUpdater {
  public static void main(String[] args) throws IOException {
    Injector injector = BasicSetup.create();

    ContentPrintDatabase store = injector.getInstance(ContentPrintDatabase.class);
    UriDatabase uriStore = injector.getInstance(UriDatabase.class);
    MediaHash mediaHash = injector.getInstance(MediaHash.class);
    AtomicInteger recordsSeen = new AtomicInteger();
    AtomicInteger hashesChanged = new AtomicInteger();
    AtomicInteger notFound = new AtomicInteger();

    store.forEach(r -> {
      List<String> uris = uriStore.findUris(r.id());
      byte[] oldHash = r.hash();

      int count = recordsSeen.incrementAndGet();
      boolean found = false;

      for(String s : uris) {
        try {
          Path path = Paths.get(URI.create(s));

          if(Files.isRegularFile(path)) {
            byte[] newHash = mediaHash.computeFileHash(path);
            found = true;

            if(!Arrays.equals(oldHash, newHash)) {
              hashesChanged.incrementAndGet();

              store.update(new ContentID(r.id()), r.size(), r.lastModificationTime(), newHash);
              break;  // No further uri's need checking if we found a working one
            }
          }
        }
        catch(IOException e) {
          System.out.println(" --> " + Throwables.formatAsOneLine(e));
        }
      }

      if(!found) {
        notFound.incrementAndGet();
      }

      System.out.println("Records processed: " + count + "; changed: " + hashesChanged.get() + "; not found: " + notFound.get());
    });
  }
}
