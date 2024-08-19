package hs.mediasystem.ext.scanners;

import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discoverer;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.scanners.NameDecoder.DecodeResult;
import hs.mediasystem.ext.scanners.NameDecoder.Mode;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;
import hs.mediasystem.util.exception.Throwables;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Singleton;

@Singleton
public class MoviesDiscoverer implements Discoverer {
  private static final Logger LOGGER = Logger.getLogger(MoviesDiscoverer.class.getName());
  private static final NameDecoder NAME_DECODER = new NameDecoder(Mode.MOVIE);
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Discovering movies");

  @Override
  public void discover(URI root, Registry registry) throws IOException {
    Path rootPath = Path.of(root);
    List<Path> scanResults = new PathFinder(1).find(rootPath, Constants.VIDEOS);
    List<Discovery> discoveries = new ArrayList<>();

    WORKLOAD.start(scanResults.size());

    for(Path path : scanResults) {
      try {
        DecodeResult result = NAME_DECODER.decode(path.getFileName().toString());

        String title = result.getTitle();
        String sequence = result.getSequence();
        String subtitle = result.getSubtitle();
        Integer year = result.getReleaseYear();

        String imdb = result.getCode();
        String imdbNumber = imdb != null && !imdb.isEmpty() ? String.format("tt%07d", Integer.parseInt(imdb)) : null;
        URI uri = path.toUri();

        Attributes attributes = Attributes.of(
          Attribute.TITLE, title,
          Attribute.ALTERNATIVE_TITLE, result.getAlternativeTitle(),
          Attribute.SUBTITLE, subtitle,
          Attribute.SEQUENCE, sequence,
          Attribute.YEAR, year == null ? null : year.toString(),
          Attribute.ID_PREFIX + "IMDB", imdbNumber
        );

        discoveries.add(new Discovery(MediaType.MOVIE, uri, attributes, Files.getLastModifiedTime(path).toInstant(), Files.size(path)));
      }
      catch(RuntimeException | IOException e) {
        LOGGER.warning("Exception while decoding item: " + path  + ", while getting items for \"" + root + "\": " + Throwables.formatAsOneLine(e));   // TODO add to some high level user error reporting facility, use Exceptional?
      }
      finally {
        WORKLOAD.complete();
      }
    }

    registry.register(null, discoveries);
  }
}
