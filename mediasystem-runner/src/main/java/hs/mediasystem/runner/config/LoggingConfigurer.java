package hs.mediasystem.runner.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggingConfigurer {
  private static final Logger LOGGER = Logger.getLogger(LoggingConfigurer.class.getName());

  public static void configure() throws SecurityException, IOException {
    try(FileInputStream stream = new FileInputStream("logging.properties")) {
      LogManager.getLogManager().readConfiguration(stream);
    }
    catch(FileNotFoundException e) {
      System.out.println("[INFO] File 'logging.properties' not found, using defaults");
    }

    LOGGER.info("Logging configured");
    LOGGER.info("Java version: " + System.getProperty("java.version"));
    LOGGER.info("Default encoding: " + Charset.defaultCharset());
  }
}
