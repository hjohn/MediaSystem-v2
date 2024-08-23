package hs.mediasystem.ext.scanners;

import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discoverer;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.scanners.NameDecoder.DecodeResult;
import hs.mediasystem.ext.scanners.NameDecoder.Mode;
import hs.mediasystem.ext.scanners.PathFinder.PathAndAttributes;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

@Singleton
public class MoviesDiscoverer implements Discoverer {
  private static final NameDecoder NAME_DECODER = new NameDecoder(Mode.MOVIE);
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Discovering movies");

  @Override
  public void discover(URI root, Registry registry) throws IOException {
    Path rootPath = Path.of(root);
    List<PathAndAttributes> scanResults = new PathFinder(1).findWithAttributes(rootPath, Constants.VIDEOS);
    List<Discovery> discoveries = new ArrayList<>();

    WORKLOAD.start(scanResults.size());

    try {
      for(PathAndAttributes pathAndAttributes : scanResults) {
        Path path = pathAndAttributes.path();
        DecodeResult result = NAME_DECODER.decode(path.getFileName().toString());

        String title = result.getTitle();
        String sequence = result.getSequence();
        String subtitle = result.getSubtitle();
        Integer year = result.getReleaseYear();

        String imdb = result.getCode();
        String imdbNumber = imdb != null && !imdb.isEmpty() ? String.format("tt%07d", Integer.parseInt(imdb)) : null;
        URI uri = Paths.appendFilePath(root, rootPath.relativize(path));

        Attributes attributes = Attributes.of(
          Attribute.TITLE, title,
          Attribute.ALTERNATIVE_TITLE, result.getAlternativeTitle(),
          Attribute.SUBTITLE, subtitle,
          Attribute.SEQUENCE, sequence,
          Attribute.YEAR, year == null ? null : year.toString(),
          Attribute.ID_PREFIX + "IMDB", imdbNumber
        );

        discoveries.add(new Discovery(MediaType.MOVIE, uri, attributes, pathAndAttributes.attrs().lastModifiedTime().toInstant(), pathAndAttributes.attrs().size()));

        WORKLOAD.complete();
      }

      registry.register(null, discoveries);
    }
    finally {
      WORKLOAD.finish();
    }
  }
}
