package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.Scanner;
import hs.mediasystem.mediamanager.StreamSource;

import java.nio.file.Path;
import java.util.List;

public class ImportSource {
  private final Scanner scanner;
  private final int id;
  private final List<Path> roots;
  private final StreamSource streamSource;

  public ImportSource(Scanner scanner, int id, List<Path> roots, StreamSource streamSource) {
    this.scanner = scanner;
    this.id = id;
    this.roots = roots;
    this.streamSource = streamSource;
  }

  public Scanner getScanner() {
    return scanner;
  }

  public List<Path> getRoots() {
    return roots;
  }

  public int getId() {
    return id;
  }

  public StreamSource getStreamSource() {
    return streamSource;
  }
}
