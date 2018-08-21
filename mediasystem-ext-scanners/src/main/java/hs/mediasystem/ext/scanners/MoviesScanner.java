package hs.mediasystem.ext.scanners;

import hs.mediasystem.ext.basicmediatypes.scan.Attribute;
import hs.mediasystem.ext.basicmediatypes.scan.MovieStream;
import hs.mediasystem.ext.basicmediatypes.scan.Scanner;
import hs.mediasystem.ext.basicmediatypes.scan.StreamPrint;
import hs.mediasystem.ext.basicmediatypes.scan.StreamPrintProvider;
import hs.mediasystem.ext.scanners.NameDecoder.DecodeResult;
import hs.mediasystem.ext.scanners.NameDecoder.Hint;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoviesScanner implements Scanner<MovieStream> {
  private static final Logger LOGGER = Logger.getLogger(MoviesScanner.class.getName());
  private static final NameDecoder NAME_DECODER = new NameDecoder(Hint.MOVIE);

  @Inject private StreamPrintProvider streamPrintProvider;

  @Override
  public List<MovieStream> scan(List<Path> roots) {
    List<MovieStream> results = new ArrayList<>();

    LOGGER.info("Scanning " + roots);

    for(Path root : roots) {
      try {
        List<Path> scanResults = new PathFinder(1).find(root);

        for(Path path : scanResults) {
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

          results.add(new MovieStream(streamPrint, attributes, Collections.emptyMap()));
        }
      }
      catch(RuntimeException | IOException e) {
        LOGGER.warning("Exception while getting items for \"" + root + "\": " + Throwables.formatAsOneLine(e));   // TODO add to some high level user error reporting facility
      }
    }

    return results;
  }
}
