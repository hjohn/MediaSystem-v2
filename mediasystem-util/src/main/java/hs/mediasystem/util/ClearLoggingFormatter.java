package hs.mediasystem.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClearLoggingFormatter extends Formatter {
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
  private static final String PIPE = "\u2502";
  private static final Map<Level, String> INDICATOR_BY_LEVEL = new HashMap<Level, String>() {{
    put(Level.SEVERE, "!!");
    put(Level.WARNING, " !");
    put(Level.INFO, "++");
    put(Level.CONFIG, " +");
    put(Level.FINE, "  ");
    put(Level.FINER, " -");
    put(Level.FINEST, "--");
  }};

  @Override
  public String format(LogRecord record) {
    StringBuilder builder = new StringBuilder();

    builder
      .append(DATE_FORMAT.format(new Date(record.getMillis())))
      .append(PIPE)
      .append(String.format("%-60s", createShortLocation(record, 50)))
      .append(PIPE)
      .append(INDICATOR_BY_LEVEL.get(record.getLevel()))
      .append(PIPE)
      .append(String.format("%-25s", Thread.currentThread().getName()))
      .append(PIPE)
      .append(formatMessage(record))
      .append("\n");

    if(record.getThrown() != null) {
      StringWriter sw = new StringWriter();

      try(PrintWriter pw = new PrintWriter(sw)) {
        record.getThrown().printStackTrace(pw);
      }

      builder.append(sw.toString()).append("\n");
    }

    return builder.toString();
  }

  private static final Pattern LONG_PACKAGE_PART = Pattern.compile("[a-z]{2,}\\.");

  public static String createShortLocation(LogRecord record, int max) {
    String full = record.getSourceClassName() + "#" + record.getSourceMethodName();

    while(full.length() > max) {
      Matcher matcher = LONG_PACKAGE_PART.matcher(full);

      if(matcher.find()) {
        full = full.substring(0, matcher.start() + 1) + full.substring(matcher.end() - 1);
        continue;
      }

      break;
    }

    return full;
  }
}
