package hs.mediasystem.util.logging;

import java.text.SimpleDateFormat;

public class DateTimeLoggingFormatter extends ClearLoggingFormatter {

  public DateTimeLoggingFormatter() {
    super(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));
  }
}
