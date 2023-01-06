package hs.mediasystem.ext.scanners;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.api.Discoverer;
import hs.mediasystem.ext.basicmediatypes.api.Discovery;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.scanners.NameDecoder.DecodeResult;
import hs.mediasystem.ext.scanners.NameDecoder.Mode;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.checked.CheckedStreams;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FoldersDiscoverer implements Discoverer {
  private static final NameDecoder FILE_NAME_DECODER = new NameDecoder(Mode.FILE);

  @Inject private ContentPrintProvider contentPrintProvider;

  @Override
  public void discover(URI root, BiConsumer<URI, List<Discovery>> consumer) throws IOException {
    scan(root, consumer, true);
  }

  private void scan(URI root, BiConsumer<URI, List<Discovery>> consumer, boolean isRoot) throws IOException {
    Path rootPath = Path.of(root);
    List<Discovery> discoveries = CheckedStreams.forIOException(Files.walk(rootPath, 1, FileVisitOption.FOLLOW_LINKS))
      .filter(path -> !path.equals(rootPath))
      .filter(path -> Files.isDirectory(path) || Constants.VIDEOS.matcher(path.getFileName().toString()).matches())
      .map(path -> toDiscovery(path, isRoot))
      .toList();

    consumer.accept(root, discoveries);

    for(Discovery discovery : discoveries) {
      if(Files.isDirectory(Path.of(discovery.location()))) {
        scan(discovery.location(), consumer, false);
      }
    }
  }

  private Discovery toDiscovery(Path path, boolean isRoot) throws IOException {
    String fileName = path.getFileName().toString();
    DecodeResult result = FILE_NAME_DECODER.decode(fileName);
    URI uri = path.toUri();
    Attributes attributes = Attributes.of(
      Attribute.TITLE, result.getTitle(),
      Attribute.SUBTITLE, result.getSubtitle(),
      Attribute.DESCRIPTION, result.getAlternativeTitle(),
      Attribute.YEAR, result.getReleaseYear()
    );

    ContentPrint contentPrint = contentPrintProvider.get(uri, Files.isDirectory(path) ? null : Files.size(path), Files.getLastModifiedTime(path).toMillis());

    return new Discovery(Files.isDirectory(path) ? MediaType.FOLDER : MediaType.FILE, uri, attributes, Optional.ofNullable(isRoot ? null : path.getParent().toUri()), contentPrint);
  }
}
