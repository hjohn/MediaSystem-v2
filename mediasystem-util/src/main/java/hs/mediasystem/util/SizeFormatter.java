package hs.mediasystem.util;

import java.util.Iterator;
import java.util.List;

public class SizeFormatter {
  public static final FormatSet<Long> BYTES_THREE_SIGNIFICANT = new FormatSet<>(List.of(
    new LongFormat("%.0f B", 1, 1000),
    new LongFormat("%.1f kB", 1024L, 100),
    new LongFormat("%.0f kB", 1024L, 1000),
    new LongFormat("%.1f MB", 1024L * 1024, 100),
    new LongFormat("%.0f MB", 1024L * 1024, 1000),
    new LongFormat("%.1f GB", 1024L * 1024 * 1024, 100),
    new LongFormat("%.0f GB", 1024L * 1024 * 1024, 1000),
    new LongFormat("%.1f TB", 1024L * 1024 * 1024 * 1024, 100),
    new LongFormat("%.0f TB", 1024L * 1024 * 1024 * 1024)
  ));

  public static final FormatSet<Long> DURATION = new FormatSet<>(List.of(
    new LongFormat("%.0f seconds", 1, 60),
    new LongFormat("%s minutes and %s seconds", 60 * 60, new LongFormat("%.0f", 60), new LongFormat("%.0f", 1)),
    new LongFormat("%s hours and %s minutes", 24 * 60 * 60, new LongFormat("%.0f", 60 * 60), new LongFormat("%.0f", 60)),
    new LongFormat("%s days and %s hours", -1, new LongFormat("%.0f", 24 * 60 * 60), new LongFormat("%.0f", 60 * 60))
  ));

  public static final FormatSet<Long> SECONDS_AS_POSITION = new FormatSet<>(List.of(
    new LongFormat("%s:%s", 60 * 60, new LongFormat("%.0f", 60), new LongFormat("%02.0f", 1)),  // m:ss
    new LongFormat("%s:%s:%s", -1, new LongFormat("%.0f", 60 * 60), new LongFormat("%02.0f", 60), new LongFormat("%02.0f", 1))  // h:mm:ss
  ));

  public static final FormatSet<Long> MILLISECONDS = new FormatSet<>(List.of(
    new LongFormat("%sd %s:%s:%s", -1, new LongFormat("%.0f", 24L * 60 * 60 * 1000), new LongFormat("%02.0f", 60L * 60 * 1000), new LongFormat("%02.0f", 60 * 1000), new LongFormat("%06.3f", 1000))
  ));

  public static final FormatSet<Double> DOUBLE_THREE_SIGNIFICANT = new FormatSet<>(List.of(
    new AutoDoubleFormat(3)
  ));

  public static String formatBytes(long bytes) {
    long b = bytes + 1023;

    return b / 1024 + " kB";
  }

  public static class FormatSet<T> implements Iterable<Format<T>> {
    private final List<Format<T>> formats;

    public FormatSet(List<Format<T>> formats) {
      this.formats = formats;
    }

    @Override
    public Iterator<Format<T>> iterator() {
      return formats.iterator();
    }

    public String format(T value) {
      for(Format<T> format : formats) {
        if(format.isApplicable(value)) {
          return format.format(value);
        }
      }

      return null;  // Code should never get here
    }
  }

  private interface Format<T> {
    String format(T value);
    boolean isApplicable(T value);
  }

  private static class LongFormat implements Format<Long> {
    private final String formatString;
    private final long cutOff;
    private final long divisor;
    private final LongFormat[] formats;

    public LongFormat(String formatString, long divisor, long cutOff) {
      this.formatString = formatString;
      this.cutOff = cutOff;
      this.divisor = divisor;
      this.formats = null;
    }

    public LongFormat(String formatString, long divisor) {
      this(formatString, divisor, -1);
    }

    public LongFormat(String formatString, long cutOff, LongFormat... formats) {
      this.formatString = formatString;
      this.cutOff = cutOff;
      this.divisor = 1;
      this.formats = formats;
    }

    @Override
    public boolean isApplicable(Long number) {
      return cutOff < 0 || number < cutOff * divisor - divisor / 2;
    }

    @Override
    public String format(Long number) {
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

  public static class AutoDoubleFormat implements Format<Double> {
    private static final String[] extensions = new String[] {" y", " z", " a", " f", " p", " n", " μ", " m", "", " k", " M", " G", " T", " P", " E", " Z", " Y"};

    private final int significantDigits;

    public AutoDoubleFormat(int significantDigits) {
      if(significantDigits < 3) {
        throw new IllegalArgumentException("Less than 3 significant digits is not supported");
      }

      this.significantDigits = significantDigits;
    }

    @Override
    public String format(Double value) {
      int exponent = (int)Math.floor(Math.log10(Math.abs(value)));
      int mod3 = exponent % 3;

      if(mod3 < 0) {
        mod3 = Math.abs(3 + mod3);
      }

      int divisorPow = exponent - mod3;

      if(divisorPow < -24) {
        divisorPow = -24;
      }
      else if(divisorPow > 24) {
        divisorPow = 24;
      }

      double n = value / Math.pow(10, divisorPow);

      int idx = divisorPow / 3 + 8;

      String extension = extensions[idx];

      if(exponent < -24) {
        return String.format("%." + (significantDigits - 1 - exponent - 24) + "f" + extension, n);
      }
      else if(exponent >= 24 + significantDigits - 1) {
        return String.format("%.0f" + extension, n);
      }

      if(mod3 == 0) {
        return String.format("%." + (significantDigits - 1) + "f" + extension, n);
      }
      if(mod3 == 1) {
        return String.format("%." + (significantDigits - 2) + "f" + extension, n);
      }

      return String.format("%." + (significantDigits - 3) + "f" + extension, n);
    }

    @Override
    public boolean isApplicable(Double value) {
      return true;
    }
  }
}
