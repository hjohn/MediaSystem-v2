package hs.mediasystem.local.client;

import hs.ddif.core.Injector;
import hs.mediasystem.db.streamids.StreamIdDatabase;
import hs.mediasystem.db.uris.UriDatabase;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.runner.config.BasicSetup;
import hs.mediasystem.util.MediaHash;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mini program to update all hash values in the database, by re-reading all
 * referenced files and setting their hash without changing their StreamID.
 */
public class HashUpdater {
  public static void main(String[] args) {
    Injector injector = BasicSetup.create();

    StreamIdDatabase store = injector.getInstance(StreamIdDatabase.class);
    UriDatabase uriStore = injector.getInstance(UriDatabase.class);
    MediaHash mediaHash = injector.getInstance(MediaHash.class);
    AtomicInteger recordsSeen = new AtomicInteger();
    AtomicInteger hashesChanged = new AtomicInteger();
    AtomicInteger notFound = new AtomicInteger();

    store.forEach(r -> {
      List<String> uris = uriStore.findUris(r.getId());
      byte[] oldHash = r.getHash();

      int count = recordsSeen.incrementAndGet();
      boolean found = false;

      for(String s : uris) {
        try {
          Path path = Paths.get(new StringURI(s).toURI());

          if(Files.isRegularFile(path)) {
            byte[] newHash = mediaHash.computeFileHash(path);
            found = true;

            if(!Arrays.equals(oldHash, newHash)) {
              hashesChanged.incrementAndGet();

              store.update(new StreamID(r.getId()), r.getSize(), r.getLastModificationTime(), newHash);
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
