package hs.mediasystem.runner;

import hs.mediasystem.db.ScannerController;
import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.ext.basicmediatypes.scan.Scanner;
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class ScannerInstaller {
  @Inject private List<Scanner<? extends MediaStream<?>>> scanners;
  @Inject private ScannerController scannerController;
  @Inject private Ini ini;

  @PostConstruct
  public void postConstruct() {
    reinstall();
  }

  public void reinstall() {
    Map<Long, Supplier<List<? extends MediaStream<?>>>> suppliers = new HashMap<>();

    Section scannersSection = ini.getSection("scanners");

    for(Scanner<? extends MediaStream<?>> scanner : scanners) {
      List<String> scannerIds = scannersSection.getAll(scanner.getClass().getSimpleName());

      for(String scannerId : scannerIds) {
        Section scannerSection = ini.getSection("scanners." + scannerId);

        suppliers.put(Long.parseUnsignedLong(scannerId, 16), () -> scanner.scan(Arrays.asList(Paths.get(scannerSection.get("path")))));
      }
    }

    scannerController.setSuppliers(suppliers);
  }
}