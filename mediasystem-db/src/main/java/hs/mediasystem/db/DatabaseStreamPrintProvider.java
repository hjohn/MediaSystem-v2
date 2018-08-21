package hs.mediasystem.db;

import hs.mediasystem.ext.basicmediatypes.scan.StreamPrint;
import hs.mediasystem.ext.basicmediatypes.scan.StreamPrintProvider;
import hs.mediasystem.util.MediaHash;
import hs.mediasystem.util.StringURI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseStreamPrintProvider implements StreamPrintProvider {
  @Inject private DatabaseStreamDataStore store;

  private Map<String, StreamPrint> streamPrints;

  @PostConstruct
  private void postConstruct() {
    streamPrints = store.findAll(sp -> toKey(sp.getUri(), sp.getSize(), sp.getLastModificationTime()));
  }

  @Override
  public StreamPrint get(StringURI uri, Long size, long lastModificationTime) throws IOException {
    String key = toKey(uri, size, lastModificationTime);

    StreamPrint streamPrint = streamPrints.get(key);

    if(streamPrint == null) {
      streamPrint = createStreamPrint(uri);

      store.store(streamPrint);
      streamPrints.put(key, streamPrint);
    }

    return streamPrint;
  }

  private static String toKey(StringURI uri, Long size, long lastModificationTime) {
    return lastModificationTime + ":" + size + ":" + uri.toString();
  }

  private static StreamPrint createStreamPrint(StringURI uri) throws IOException {
    Path path = Paths.get(uri.toURI());

    if(Files.isRegularFile(path)) {
      byte[] hash = MediaHash.loadMediaHash(path);
      long osHash = MediaHash.loadOpenSubtitlesHash(path);

      return new StreamPrint(uri, Files.size(path), Files.getLastModifiedTime(path).toMillis(), hash, osHash);
    }

    return new StreamPrint(uri, null, Files.getLastModifiedTime(path).toMillis(), MediaHash.createHash(path), null);
  }
}
