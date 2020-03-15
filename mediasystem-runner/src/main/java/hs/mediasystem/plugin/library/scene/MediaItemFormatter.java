package hs.mediasystem.plugin.library.scene;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;

public class MediaItemFormatter {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

  public static StringBinding formattedLocalDate(final ObservableValue<LocalDate> date) {
    return new StringBinding() {
      {
        bind(date);
      }

      @Override
      protected String computeValue() {
        return formattedLocalDate(date.getValue());
      }
    };
  }

  public static String formattedLocalDate(LocalDate date) {
    return date == null ? null : DATE_TIME_FORMATTER.format(date);
  }
}
