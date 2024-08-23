package hs.mediasystem.ext.scanners;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class PathFinder {
  record PathAndAttributes(Path path, BasicFileAttributes attrs) {}

  private static final Set<FileVisitOption> FOLLOW_LINKS = Set.of(FileVisitOption.FOLLOW_LINKS);

  private final int maxDepth;

  public PathFinder(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public List<Path> find(Path scanPath, Pattern pattern) throws IOException {
    List<Path> results = new ArrayList<>();

    Files.walkFileTree(scanPath, FOLLOW_LINKS, maxDepth, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if(!attrs.isDirectory() && pattern.matcher(file.getFileName().toString()).matches()) {
          results.add(file);
        }

        return FileVisitResult.CONTINUE;
      }
    });

    return results;
  }

  public List<PathAndAttributes> findWithAttributes(Path scanPath, Pattern pattern) throws IOException {
    List<PathAndAttributes> results = new ArrayList<>();

    Files.walkFileTree(scanPath, FOLLOW_LINKS, maxDepth, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if(!attrs.isDirectory() && pattern.matcher(file.getFileName().toString()).matches()) {
          results.add(new PathAndAttributes(file, attrs));
        }

        return FileVisitResult.CONTINUE;
      }
    });

    return results;
  }
}
