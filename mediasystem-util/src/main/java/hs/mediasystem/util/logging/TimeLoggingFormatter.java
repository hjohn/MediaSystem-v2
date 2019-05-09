package hs.mediasystem.util.logging;

import java.text.SimpleDateFormat;

public class TimeLoggingFormatter extends ClearLoggingFormatter {

  public TimeLoggingFormatter() {
    super(new SimpleDateFormat("HH:mm:ss.SSS"));
  }
}
