package hs.mediasystem.util;

import java.util.Arrays;
import java.util.Iterator;

public class SizeFormatter {
  public static final FormatSet BYTES_THREE_SIGNIFICANT = new FormatSet(new Format[] {
    new Format("%.0f B", 1, 1000),
    new Format("%.1f kB", 1024L, 100),
    new Format("%.0f kB", 1024L, 1000),
    new Format("%.1f MB", 1024L * 1024, 100),
    new Format("%.0f MB", 1024L * 1024, 1000),
    new Format("%.1f GB", 1024L * 1024 * 1024, 100),
    new Format("%.0f GB", 1024L * 1024 * 1024, 1000),
    new Format("%.1f TB", 1024L * 1024 * 1024 * 1024, 100),
    new Format("%.0f TB", 1024L * 1024 * 1024 * 1024)
  });

  public static final FormatSet DURATION = new FormatSet(new Format[] {
    new Format("%.0f seconds", 1, 60),
    new Format("%s minutes and %s seconds", 60 * 60, new Format("%.0f", 60), new Format("%.0f", 1)),
    new Format("%s hours and %s minutes", 24 * 60 * 60, new Format("%.0f", 60 * 60), new Format("%.0f", 60)),
    new Format("%s days and %s hours", -1, new Format("%.0f", 24 * 60 * 60), new Format("%.0f", 60 * 60))
  });

  public static final FormatSet SECONDS_AS_POSITION = new FormatSet(new Format[] {
    new Format("%s:%s", 60 * 60, new Format("%.0f", 60), new Format("%02.0f", 1)),  // m:ss
    new Format("%s:%s:%s", -1, new Format("%.0f", 60 * 60), new Format("%02.0f", 60), new Format("%02.0f", 1))  // h:mm:ss
  });

  public static final FormatSet MILLISECONDS = new FormatSet(new Format[] {
    new Format("%sd %s:%s:%s", -1, new Format("%.0f", 24L * 60 * 60 * 1000), new Format("%02.0f", 60L * 60 * 1000), new Format("%02.0f", 60 * 1000), new Format("%06.3f", 1000))
  });

  public static String formatBytes(long bytes) {
    long b = bytes + 1023;

    return b / 1024 + " kB";
  }

  public static class FormatSet implements Iterable<Format> {
    private final Format[] formats;

    public FormatSet(Format... formats) {
      this.formats = formats;
    }

    @Override
    public Iterator<Format> iterator() {
      return Arrays.asList(formats).iterator();
    }

    public String format(long number) {
      for(Format format : formats) {
        if(format.isApplicable(number)) {
          return format.format(number);
        }
      }

      return null;  // Code should never get here
    }
  }

  private static class Format {
    private final String formatString;
    private final long cutOff;
    private final long divisor;
    private final Format[] formats;

    public Format(String formatString, long divisor, long cutOff) {
      this.formatString = formatString;
      this.cutOff = cutOff;
      this.divisor = divisor;
      this.formats = null;
    }

    public Format(String formatString, long divisor) {
      this(formatString, divisor, -1);
    }

    public Format(String formatString, long cutOff, Format... formats) {
      this.formatString = formatString;
      this.cutOff = cutOff;
      this.divisor = 1;
      this.formats = formats;
    }

    public boolean isApplicable(long number) {
      return cutOff < 0 || number < cutOff * divisor - divisor / 2;
    }

    public String format(long number) {
      if(formats == null) {
        return String.format(formatString, ((double)number) / divisor);
      }

      String[] args = new String[formats.length];
      long n = number;

      for(int i = 0; i < formats.length; i++) {
        long mod = i == formats.length - 1 ? 0 : n % formats[i].divisor;
        args[i] = formats[i].format(n - mod);
        n = mod;
      }

      return String.format(formatString, (Object[])args);
    }
  }
}
