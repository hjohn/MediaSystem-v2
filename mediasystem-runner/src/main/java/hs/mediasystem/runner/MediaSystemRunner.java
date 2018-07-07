package hs.mediasystem.runner;

import java.io.IOException;

public class MediaSystemRunner {

  public static void main(String[] args) throws SecurityException, IOException {
    MediaSystemConfigurer.start();
  }

  // ScannerController: reads information from settings(??) and scans periodically using correct scanner some dir and feeds to MediaManager
  // ScannerControllerUI: exposes or sets up info for creating settings UI

  //ScannerController controller = injector.getInstance(ScannerController.class);



}