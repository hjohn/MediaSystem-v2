package hs.mediasystem.util.logging;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;

public class PathCreatingFileHandler extends FileHandler {

  public PathCreatingFileHandler() throws SecurityException, IOException {
    super();
  }

  @Override
  public synchronized void setEncoding(String encoding) throws SecurityException, UnsupportedEncodingException {
    super.setEncoding(encoding);

    // Incredibly dirty hack to check the path and create directories
    // just after encoding is set.  This may stop working in the future.

    LogManager manager = LogManager.getLogManager();
    String pattern = manager.getProperty(getClass().getName() + ".pattern");

    if(pattern == null) {
      pattern = "%h/java%u.log";  // default to use home directory
    }

    createDestinationDirectory(pattern);
  }

  private static void createDestinationDirectory(String pattern) {
    int index = pattern.lastIndexOf('/');

    if(index != -1) {
      String dir = pattern.substring(0, index);

      dir = dir.replaceAll("%h", System.getProperty("user.home"));
      dir = dir.replaceAll("%t", System.getProperty("java.io.tmpdir"));

      new File(dir).mkdirs();
    }
  }
}
