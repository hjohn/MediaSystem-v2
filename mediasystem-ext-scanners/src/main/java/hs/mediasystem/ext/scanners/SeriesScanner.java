package hs.mediasystem.ext.scanners;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute.ChildType;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Scanner;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.scanners.NameDecoder.DecodeResult;
import hs.mediasystem.ext.scanners.NameDecoder.Mode;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.RuntimeIOException;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class SeriesScanner implements Scanner {
  private static final Logger LOGGER = Logger.getLogger(MoviesScanner.class.getName());
  private static final NameDecoder NAME_DECODER = new NameDecoder(Mode.SERIE);
  private static final NameDecoder EPISODE_NAME_DECODER = new NameDecoder(Mode.EPISODE);
  private static final NameDecoder SPECIAL_NAME_DECODER = new NameDecoder(Mode.SPECIAL);
  private static final NameDecoder SIMPLE_NAME_DECODER = new NameDecoder(Mode.SIMPLE);
  private static final Set<FileVisitOption> FILE_VISIT_OPTIONS = new HashSet<>(Arrays.asList(FileVisitOption.FOLLOW_LINKS));
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Scanning series");
	private static final MediaType SERIE = MediaType.of("SERIE");
	private static final MediaType EPISODE = MediaType.of("EPISODE");
	
  @Inject private StreamPrintProvider streamPrintProvider;

  @Override
  public List<Exceptional<List<Streamable>>> scan(List<Path> roots) {
    List<Exceptional<List<Streamable>>> rootResults = new ArrayList<>();

    LOGGER.info("Scanning " + roots);

    for(Path root : roots) {
      try {
        List<Path> scanResults = scan(root);

        WORKLOAD.start(scanResults.size());

        List<Streamable> results = new ArrayList<>();

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

          results.add(new Streamable(SERIE, uri, streamPrint.getId(), null, attributes));
          results.addAll(scanSerie(path, streamPrint.getId()));

          WORKLOAD.complete();
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

  private List<Streamable> scanSerie(Path root, StreamID parentId) {
    List<Streamable> results = new ArrayList<>();

    try {
      List<Path> scanResults = new PathFinder(5).find(root);

      for(Path path : scanResults) {
        Path relative = root.relativize(path);
        ChildType type = hasPathPart(relative, "specials?") ? ChildType.SPECIAL :
                         hasPathPart(relative, "extras?")   ? ChildType.EXTRA : null;

        DecodeResult result = type == ChildType.SPECIAL ? SPECIAL_NAME_DECODER.decode(path.getFileName().toString()) :
                              type == ChildType.EXTRA   ? SIMPLE_NAME_DECODER.decode(path.getFileName().toString()) :
                                                          EPISODE_NAME_DECODER.decode(path.getFileName().toString());

        String title = result.getTitle();
        String subtitle = result.getSubtitle();
        String sequence = result.getSequence();
        Integer year = result.getReleaseYear();

        String imdb = result.getCode();
        String imdbNumber = imdb != null && !imdb.isEmpty() ? String.format("tt%07d", Integer.parseInt(imdb)) : null;

        StreamPrint streamPrint = streamPrintProvider.get(new StringURI(path.toUri()), Files.size(path), Files.getLastModifiedTime(path).toMillis());

        if(type == null && sequence != null && sequence.contains(",")) {
          type = ChildType.EPISODE;  // sequences with a comma have an episode number in them (",2", "10,15"); ones without comma only had a season or special number in it, or nothing at all
        }

        Attributes attributes = Attributes.of(
          Attribute.TITLE, title,
          Attribute.SUBTITLE, subtitle,
          Attribute.SEQUENCE, sequence,
          Attribute.YEAR, year == null ? null : year.toString(),
          Attribute.ID_PREFIX + "IMDB", imdbNumber,
          Attribute.CHILD_TYPE, type == null ? null : type.toString()
        );

        results.add(new Streamable(EPISODE, new StringURI(path.toUri()), streamPrint.getId(), parentId, attributes));
      }
    }
    catch(RuntimeException | IOException e) {
      LOGGER.warning("Exception while getting items for \"" + root + "\": " + Throwables.formatAsOneLine(e));   // TODO add to some high level user error reporting facility
    }

    return results;
  }

  private static boolean hasPathPart(Path input, String part) {
    Pattern prefixPattern = Pattern.compile(part + "\\b");
    Pattern postfixPattern = Pattern.compile("\\b" + part);

    for(Path path : input) {
      String name = path.toString().toLowerCase();

      if(prefixPattern.matcher(name).matches() || postfixPattern.matcher(name).matches()) {
        return true;
      }
    }

    return false;
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
