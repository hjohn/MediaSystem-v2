package hs.mediasystem.ext.scanners;

import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discoverer;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.domain.stream.MediaType;
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
import java.util.stream.Stream;

import javax.inject.Singleton;

@Singleton
public class FoldersDiscoverer implements Discoverer {
  private static final NameDecoder FILE_NAME_DECODER = new NameDecoder(Mode.FILE);

  @Override
  public void discover(URI root, Registry registry) throws IOException {
    scan(root, registry, true);
  }

  private void scan(URI root, Registry registry, boolean isRoot) throws IOException {
    Path rootPath = Path.of(root);

    try(Stream<Path> walk = Files.walk(rootPath, 1, FileVisitOption.FOLLOW_LINKS)) {
      List<Discovery> discoveries = CheckedStreams.forIOException(walk)
        .filter(path -> !path.equals(rootPath))
        .filter(path -> Files.isDirectory(path) || Constants.VIDEOS.matcher(path.getFileName().toString()).matches())
        .map(path -> toDiscovery(path))
        .toList();

      registry.register(isRoot ? null : root, discoveries);

      for(Discovery child : discoveries) {
        if(Files.isDirectory(Path.of(child.location()))) {
          scan(child.location(), registry, false);
        }
      }
    }
  }

  private static Discovery toDiscovery(Path path) throws IOException {
    String fileName = path.getFileName().toString();
    DecodeResult result = FILE_NAME_DECODER.decode(fileName);
    URI uri = path.toUri();
    Attributes attributes = Attributes.of(
      Attribute.TITLE, result.getTitle(),
      Attribute.SUBTITLE, result.getSubtitle(),
      Attribute.DESCRIPTION, result.getAlternativeTitle(),
      Attribute.YEAR, result.getReleaseYear()
    );

    return new Discovery(
      Files.isDirectory(path) ? MediaType.FOLDER : MediaType.FILE,
      uri,
      attributes,
      Files.getLastModifiedTime(path).toInstant(),
      Files.isDirectory(path) ? null : Files.size(path)
    );
  }
}
