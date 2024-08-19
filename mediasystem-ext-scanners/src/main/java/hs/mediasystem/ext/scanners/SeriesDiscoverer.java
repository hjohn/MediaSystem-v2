package hs.mediasystem.ext.scanners;

import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Attribute.ChildType;
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

import javax.inject.Singleton;

@Singleton
public class SeriesDiscoverer implements Discoverer {
  private static final Logger LOGGER = Logger.getLogger(SeriesDiscoverer.class.getName());
  private static final Pattern SPECIALS_PATTERN = Pattern.compile("(?i)(\\bspecials?\\b.*|.*\\bspecials?\\b)");
  private static final Pattern EXTRAS_PATTERN = Pattern.compile("(?i)(\\bextras?\\b.*|.*\\bextras?\\b)");
  private static final NameDecoder NAME_DECODER = new NameDecoder(Mode.SERIE);
  private static final NameDecoder EPISODE_NAME_DECODER = new NameDecoder(Mode.EPISODE);
  private static final NameDecoder SPECIAL_NAME_DECODER = new NameDecoder(Mode.SPECIAL);
  private static final NameDecoder SIMPLE_NAME_DECODER = new NameDecoder(Mode.SIMPLE);
  private static final Set<FileVisitOption> FILE_VISIT_OPTIONS = new HashSet<>(Arrays.asList(FileVisitOption.FOLLOW_LINKS));
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Discovering series");

  @Override
  public void discover(URI root, Registry registry) throws IOException {
    List<Path> scanResults = scan(Path.of(root));
    List<Discovery> series = new ArrayList<>();

    WORKLOAD.start(scanResults.size());

    for(Path path : scanResults) {
      try {
        DecodeResult result = NAME_DECODER.decode(path.getFileName().toString());

        String title = result.getTitle();
        String subtitle = result.getSubtitle();
        Integer year = result.getReleaseYear();

        String imdb = result.getCode();
        String imdbNumber = imdb != null && !imdb.isEmpty() ? String.format("tt%07d", Integer.parseInt(imdb)) : null;

        Attributes attributes = Attributes.of(
          Attribute.TITLE, title,
          Attribute.SUBTITLE, subtitle,
          Attribute.YEAR, year == null ? null : year.toString(),
          Attribute.ID_PREFIX + "IMDB", imdbNumber
        );

        series.add(new Discovery(MediaType.SERIE, path.toUri(), attributes, Files.getLastModifiedTime(path).toInstant(), null));
      }
      catch(Exception e) {
        LOGGER.warning("Exception while decoding item: " + path  + ", while getting items for \"" + root + "\": " + Throwables.formatAsOneLine(e));   // TODO add to some high level user error reporting facility, use Exceptional?
      }
    }

    registry.register(null, series);

    for(Discovery discovery : series) {
      try {
        registry.register(discovery.location(), scanSerie(discovery.location()));
      }
      catch(Exception e) {
        LOGGER.warning("Exception while scanning serie: " + discovery  + ", for \"" + root + "\": " + Throwables.formatAsOneLine(e));   // TODO add to some high level user error reporting facility, use Exceptional?
      }
      finally {
        WORKLOAD.complete();
      }
    }
  }

  private static List<Discovery> scanSerie(URI root) {
    List<Discovery> results = new ArrayList<>();
    Path rootPath = Path.of(root);

    try {
      List<Path> scanResults = new PathFinder(5).find(rootPath, Constants.VIDEOS);

      for(Path path : scanResults) {
        Path relative = rootPath.relativize(path);
        String name = path.getFileName().toString();
        ChildType type = hasPathPart(relative, SPECIALS_PATTERN) ? ChildType.SPECIAL :
                         hasPathPart(relative, EXTRAS_PATTERN)   ? ChildType.EXTRA : null;

        DecodeResult result = type == ChildType.SPECIAL ? SPECIAL_NAME_DECODER.decode(name) :
                              type == ChildType.EXTRA   ? SIMPLE_NAME_DECODER.decode(name) :
                                                          EPISODE_NAME_DECODER.decode(name);

        String title = result.getTitle();
        String subtitle = result.getSubtitle();
        String sequence = result.getSequence();
        Integer year = result.getReleaseYear();

        String imdb = result.getCode();
        String imdbNumber = imdb != null && !imdb.isEmpty() ? String.format("tt%07d", Integer.parseInt(imdb)) : null;

        URI uri = path.toUri();

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

        results.add(new Discovery(MediaType.EPISODE, uri, attributes, Files.getLastModifiedTime(path).toInstant(), Files.size(path)));
      }
    }
    catch(RuntimeException | IOException e) {
      LOGGER.warning("Exception while getting items for \"" + rootPath + "\": " + Throwables.formatAsOneLine(e));   // TODO add to some high level user error reporting facility
    }

    return results;
  }

  private static boolean hasPathPart(Path input, Pattern pattern) {
    for(Path path : input) {
      String name = path.toString();

      if(pattern.matcher(name).matches()) {
        return true;
      }
    }

    return false;
  }

  private static List<Path> scan(Path scanPath) throws IOException {
    List<Path> results = new ArrayList<>();

    Files.walkFileTree(scanPath, FILE_VISIT_OPTIONS, 2, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
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
}
