package hs.mediasystem.runner;

import hs.ddif.core.Injector;
import hs.mediasystem.runner.config.BasicSetup;
import hs.mediasystem.runner.config.LoggingConfigurer;
import hs.mediasystem.runner.config.MediaSystemConfigurer;

import java.io.IOException;

public class MediaSystemRunner {

  public static void main(String[] args) throws SecurityException, IOException {
    LoggingConfigurer.configure();

    Injector injector = BasicSetup.create();

    MediaSystemConfigurer.configure(injector);
  }
}