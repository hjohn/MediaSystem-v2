package hs.mediasystem.ext.scanners;

import hs.mediasystem.ext.basicmediatypes.Attribute;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaRecord;
import hs.mediasystem.ext.basicmediatypes.MovieDescriptor;
import hs.mediasystem.ext.basicmediatypes.MovieStream;
import hs.mediasystem.ext.basicmediatypes.Scanner;
import hs.mediasystem.ext.basicmediatypes.StreamPrint;
import hs.mediasystem.ext.basicmediatypes.StreamPrintProvider;
import hs.mediasystem.ext.basicmediatypes.Type;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoviesScanner implements Scanner<MovieStream> {
  private static final Logger LOGGER = Logger.getLogger(MoviesScanner.class.getName());
  private static final NameDecoder NAME_DECODER = new NameDecoder(Hint.MOVIE);
  private static final Type MOVIE_TYPE = Type.of("MOVIE");
  private static final DataSource IMDB_DATA_SOURCE = DataSource.instance(MOVIE_TYPE, "IMDB");

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
          Map<Identifier, MediaRecord<MovieDescriptor>> mediaRecords = imdbNumber == null ? Collections.emptyMap() : new HashMap<>();

          if(imdbNumber != null) {
            Identifier imdbIdentifier = new Identifier(IMDB_DATA_SOURCE, imdbNumber);

            mediaRecords.put(imdbIdentifier, new MediaRecord<>(new Identification(imdbIdentifier, MatchType.ID, 1.0), null));
          }

          Attributes attributes = Attributes.of(
            Attribute.TITLE, title,
            Attribute.SUBTITLE, subtitle,
            Attribute.SEQUENCE, sequence,
            Attribute.YEAR, year == null ? null : year.toString(),
            Attribute.ID_PREFIX + "IMDB", imdbNumber
          );

          results.add(new MovieStream(streamPrint, attributes, mediaRecords));
        }
      }
      catch(RuntimeException | IOException e) {
        LOGGER.warning("Exception while getting items for \"" + root + "\": " + Throwables.formatAsOneLine(e));   // TODO add to some high level user error reporting facility
      }
    }

    return results;
  }
}
