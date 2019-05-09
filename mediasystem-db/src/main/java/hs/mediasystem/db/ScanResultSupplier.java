package hs.mediasystem.db;

import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.Scanner;
import hs.mediasystem.util.Exceptional;

import java.nio.file.Path;
import java.util.List;

public class ScanResultSupplier {
  private final Scanner scanner;
  private final int id;
  private final List<Path> roots;
  private final StreamTags tags;
  private final String dataSourceName;

  public ScanResultSupplier(Scanner scanner, int id, List<Path> roots, StreamTags tags, String dataSourceName) {
    this.scanner = scanner;
    this.id = id;
    this.roots = roots;
    this.tags = tags;
    this.dataSourceName = dataSourceName;
  }

  public String getName() {
    return scanner.getClass().getSimpleName();
  }

  public List<Exceptional<List<BasicStream>>> scan() {
    return scanner.scan(roots);
  }

  public List<Path> getRoots() {
    return roots;
  }

  public int getId() {
    return id;
  }

  public StreamTags getStreamTags() {
    return tags;
  }
}
