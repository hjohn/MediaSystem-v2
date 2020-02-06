package hs.mediasystem.ext.scanners;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Scanner;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.scanners.NameDecoder.DecodeResult;
import hs.mediasystem.ext.scanners.NameDecoder.Mode;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoviesScanner implements Scanner {
  private static final Logger LOGGER = Logger.getLogger(MoviesScanner.class.getName());
  private static final NameDecoder NAME_DECODER = new NameDecoder(Mode.MOVIE);
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Scanning movies");

  @Inject private StreamPrintProvider streamPrintProvider;

  @Override
  public List<Exceptional<List<Streamable>>> scan(List<Path> roots) {
    List<Exceptional<List<Streamable>>> rootResults = new ArrayList<>();

    LOGGER.info("Scanning " + roots);

    for(Path root : roots) {
      try {
        List<Path> scanResults = new PathFinder(1).find(root);
        List<Streamable> results = new ArrayList<>();

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

            StreamPrint streamPrint = streamPrintProvider.get(new StringURI(uri), Files.size(path), Files.getLastModifiedTime(path).toMillis());

            Attributes attributes = Attributes.of(
              Attribute.TITLE, title,
              Attribute.ALTERNATIVE_TITLE, result.getAlternativeTitle(),
              Attribute.SUBTITLE, subtitle,
              Attribute.SEQUENCE, sequence,
              Attribute.YEAR, year == null ? null : year.toString(),
              Attribute.ID_PREFIX + "IMDB", imdbNumber
            );

            results.add(new Streamable(MediaType.of("MOVIE"), new StringURI(uri), streamPrint.getId(), null, attributes));
          }
          catch(RuntimeException | IOException e) {
            LOGGER.warning("Exception while decoding item: " + path  + ", while getting items for \"" + root + "\": " + Throwables.formatAsOneLine(e));   // TODO add to some high level user error reporting facility, use Exceptional?
          }
          finally {
            WORKLOAD.complete();
          }
        }

        rootResults.add(Exceptional.of(results));
      }
      catch(RuntimeException | IOException e) {
        WORKLOAD.reset();

        rootResults.add(Exceptional.ofException(e));
      }
    }

    return rootResults;
  }
}
