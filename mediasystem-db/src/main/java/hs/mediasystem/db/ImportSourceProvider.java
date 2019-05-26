package hs.mediasystem.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Singleton;

@Singleton
public class ImportSourceProvider {
  private List<ImportSource> importSources = List.of();
  private Map<Integer, ImportSource> importSourcesByScannerId = Map.of();

  public synchronized void set(List<ImportSource> importSources) {
    this.importSources = Collections.unmodifiableList(new ArrayList<>(importSources));
    this.importSourcesByScannerId = Collections.unmodifiableMap(importSources.stream().collect(Collectors.toMap(ImportSource::getId, Function.identity())));
  }

  public synchronized ImportSource getStreamSource(int scannerId) {
    return importSourcesByScannerId.get(scannerId);
  }

  public synchronized List<ImportSource> getImportSources() {
    return importSources;
  }
}
