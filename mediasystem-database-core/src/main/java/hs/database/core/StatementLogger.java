package hs.database.core;

import hs.database.core.Database.Transaction;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StatementLogger {
  private static final Logger LOGGER = Logger.getLogger(Database.class.getName());
  private static final AutoDoubleFormat FORMAT = new AutoDoubleFormat(3);

  public static void log(Transaction tx, String sql, int rows, long bytesTransferred, long nanos) {
    if(LOGGER.isLoggable(Level.FINE)) {
      StringBuilder builder = new StringBuilder();

      builder.append(tx);
      builder.append(" [");

      if(nanos > 0) {
        builder.append(nanos / 1000000);
        builder.append(" ms, ");
      }

      builder.append(rows);
      builder.append(rows == 1 ? " row" : " rows");

      if(nanos > 0 && rows > 1) {
        builder.append(", ");
        builder.append(rows * 1000_000_000L / nanos);
        builder.append(" rows/s");
      }

      if(bytesTransferred > 0) {
        builder.append(", ");
        builder.append(FORMAT.format((double)bytesTransferred));
        builder.append("B");

        if(nanos > 0) {
          builder.append(", ");
          builder.append(FORMAT.format(bytesTransferred * 1000_000_000.0 / nanos));
          builder.append("B/s");
        }
      }

      builder.append("] ");
      builder.append(sql);

      LOGGER.fine(builder.toString());
    }
  }

  static class AutoDoubleFormat {
    private static final String[] extensions = new String[] {" y", " z", " a", " f", " p", " n", " μ", " m", "", " k", " M", " G", " T", " P", " E", " Z", " Y"};

    private final int significantDigits;

    public AutoDoubleFormat(int significantDigits) {
      if(significantDigits < 3) {
        throw new IllegalArgumentException("Less than 3 significant digits is not supported");
      }

      this.significantDigits = significantDigits;
    }

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

      return String.format("%." + (significantDigits - mod3 - 1) + "f" + extension, n);
    }
  }
}
