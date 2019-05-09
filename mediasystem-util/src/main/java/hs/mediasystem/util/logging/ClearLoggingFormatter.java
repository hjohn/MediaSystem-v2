package hs.mediasystem.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClearLoggingFormatter extends Formatter {
  private static final String PIPE = "\u2502";
  private static final Map<Level, String> INDICATOR_BY_LEVEL = new HashMap<>() {{
    put(Level.SEVERE, "\u203C");
    put(Level.WARNING, "!");
    put(Level.INFO, " ");
    put(Level.CONFIG, "\u00a9");
    put(Level.FINE, "-");
    put(Level.FINER, "=");
    put(Level.FINEST, "\u2261");
  }};

  private final String emptyLine;
  private final DateFormat dateFormat;

  public ClearLoggingFormatter(DateFormat dateFormat) {
    this.dateFormat = dateFormat;
    this.emptyLine = String.format("%" + this.dateFormat.format(new Date()).length() + "s" + PIPE + "%60s" + PIPE + "%1s" + PIPE + "%25s" + PIPE, "", "", "", "");
  }

  @Override
  public String format(LogRecord record) {
    StringBuilder builder = new StringBuilder();

    builder
      .append(dateFormat.format(new Date(record.getMillis())))
      .append(PIPE)
      .append(String.format("%-60s", createShortLocation(record, 60)))
      .append(PIPE)
      .append(INDICATOR_BY_LEVEL.get(record.getLevel()))
      .append(PIPE)
      .append(String.format("%-25s", Thread.currentThread().getName()))
      .append(PIPE);

    appendMessage(record, builder);

    builder.append("\n");

    if(record.getThrown() != null) {
      StringWriter sw = new StringWriter();

      try(PrintWriter pw = new PrintWriter(sw)) {
        record.getThrown().printStackTrace(pw);
      }

      builder.append(sw.toString()).append("\n");
    }

    return builder.toString();
  }

  private void appendMessage(LogRecord record, StringBuilder builder) {
    String message = record.getMessage();

    int lineBreak = message.indexOf('\n');

    if(lineBreak >= 0) {
      int start = 0;
      builder.append(message.substring(start, lineBreak));

      do {
        builder.append("\n");
        builder.append(emptyLine);

        start = lineBreak + 1;
        lineBreak = message.indexOf('\n', start);

        builder.append(message.substring(start, lineBreak == -1 ? message.length() : lineBreak));
      } while(lineBreak != -1);
    }
    else {
      builder.append(message);
    }
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
