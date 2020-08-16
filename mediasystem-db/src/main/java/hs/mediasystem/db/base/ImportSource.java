package hs.mediasystem.db.base;

import hs.mediasystem.ext.basicmediatypes.domain.stream.Scanner;
import hs.mediasystem.mediamanager.StreamSource;

import java.nio.file.Path;

/**
 * Represents a source of media to be imported.  This includes how this source
 * should be scanned, and after scanning, how each items should be tagged and
 * (optionally) identified.
 */
public class ImportSource {
  private final Scanner scanner;
  private final int id;
  private final Path root;
  private final StreamSource streamSource;

  public ImportSource(Scanner scanner, int id, Path root, StreamSource streamSource) {
    this.scanner = scanner;
    this.id = id;
    this.root = root;
    this.streamSource = streamSource;
  }

  public Scanner getScanner() {
    return scanner;
  }

  public Path getRoot() {
    return root;
  }

  public int getId() {
    return id;
  }

  public StreamSource getStreamSource() {
    return streamSource;
  }

  @Override
  public String toString() {
    return "ImportSource[id=" + id + ", scannerClass=" + scanner.getClass() + ", streamSource=" + streamSource + ", root=" + root + "]";
  }
}
