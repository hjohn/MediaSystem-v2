package hs.mediasystem.ext.scanners;

import hs.mediasystem.ext.basicmediatypes.Attribute;
import hs.mediasystem.ext.basicmediatypes.EpisodeStream;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.Scanner;
import hs.mediasystem.ext.basicmediatypes.SerieStream;
import hs.mediasystem.ext.basicmediatypes.StreamPrint;
import hs.mediasystem.ext.basicmediatypes.StreamPrintProvider;
import hs.mediasystem.ext.scanners.NameDecoder.DecodeResult;
import hs.mediasystem.ext.scanners.NameDecoder.Hint;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.RuntimeIOException;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;

public class SeriesScanner implements Scanner<MediaStream<?>> {
  private static final Logger LOGGER = Logger.getLogger(MoviesScanner.class.getName());
  private static final NameDecoder NAME_DECODER = new NameDecoder(Hint.MOVIE, Hint.FOLDER_NAMES);
  private static final NameDecoder EPISODE_NAME_DECODER = new NameDecoder(Hint.EPISODE);
  private static final Set<FileVisitOption> FILE_VISIT_OPTIONS = new HashSet<>(Arrays.asList(FileVisitOption.FOLLOW_LINKS));

  @Inject private StreamPrintProvider streamPrintProvider;

  @Override
  public List<MediaStream<?>> scan(List<Path> roots) {
    List<MediaStream<?>> results = new ArrayList<>();

    LOGGER.info("Scanning " + roots);

    for(Path root : roots) {
      try {
        List<Path> scanResults = scan(root);

        for(Path path : scanResults) {
          DecodeResult result = NAME_DECODER.decode(path.getFileName().toString());

          String title = result.getTitle();
          String subtitle = result.getSubtitle();
          Integer year = result.getReleaseYear();

          String imdb = result.getCode();
          String imdbNumber = imdb != null && !imdb.isEmpty() ? String.format("tt%07d", Integer.parseInt(imdb)) : null;
          StringURI uri = new StringURI(path.toUri());

          StreamPrint streamPrint = streamPrintProvider.get(uri, null, Files.getLastModifiedTime(path).toMillis());

          Attributes attributes = Attributes.of(
            Attribute.TITLE, title,
            Attribute.SUBTITLE, subtitle,
            Attribute.YEAR, year == null ? null : year.toString(),
            Attribute.ID_PREFIX + "IMDB", imdbNumber
          );

          results.add(new SerieStream(streamPrint, attributes, Collections.emptyMap()));

          results.addAll(scanSerie(path, uri));
        }
      }
      catch(RuntimeException | IOException e) {
        LOGGER.warning("Exception while getting items for \"" + root + "\": " + Throwables.formatAsOneLine(e));   // TODO add to some high level user error reporting facility
      }
    }

    return results;
  }

  private List<MediaStream<?>> scanSerie(Path root, StringURI parentUri) {
    List<MediaStream<?>> results = new ArrayList<>();

    try {
      List<Path> scanResults = new PathFinder(2).find(root);

      for(Path path : scanResults) {
        DecodeResult result = EPISODE_NAME_DECODER.decode(path.getFileName().toString());

        String title = result.getTitle();
        String subtitle = result.getSubtitle();
        String sequence = result.getSequence();
        Integer year = result.getReleaseYear();

        String imdb = result.getCode();
        String imdbNumber = imdb != null && !imdb.isEmpty() ? String.format("tt%07d", Integer.parseInt(imdb)) : null;

        StreamPrint streamPrint = streamPrintProvider.get(new StringURI(path.toUri()), Files.size(path), Files.getLastModifiedTime(path).toMillis());

        Attributes attributes = Attributes.of(
          Attribute.TITLE, title,
          Attribute.SUBTITLE, subtitle,
          Attribute.SEQUENCE, sequence,
          Attribute.YEAR, year == null ? null : year.toString(),
          Attribute.ID_PREFIX + "IMDB", imdbNumber
        );

        // TODO PARENT!
        results.add(new EpisodeStream(parentUri, streamPrint, attributes, Collections.emptyMap()));
      }
    }
    catch(RuntimeException | IOException e) {
      LOGGER.warning("Exception while getting items for \"" + root + "\": " + Throwables.formatAsOneLine(e));   // TODO add to some high level user error reporting facility
    }

    return results;
  }

  private static List<Path> scan(Path scanPath) {
    try {
      List<Path> results = new ArrayList<>();

      Files.walkFileTree(scanPath, FILE_VISIT_OPTIONS, 2, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          if(dir.equals(scanPath)) {
            return FileVisitResult.CONTINUE;
          }

          if(!dir.getFileName().toString().startsWith(".")) {
            results.add(dir);
          }

          return FileVisitResult.SKIP_SUBTREE;
        }
      });

      return results;
    }
    catch(IOException e) {
      throw new RuntimeIOException("Exception while scanning \"" + scanPath + "\"", e);
    }
  }
}
