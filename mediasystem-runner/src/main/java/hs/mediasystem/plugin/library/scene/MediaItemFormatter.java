package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.util.javafx.MapBindings;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;

public class MediaItemFormatter {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

  public static StringBinding releaseTimeBinding(final ObservableValue<Media> media) {
    return new StringBinding() {
      final ObjectBinding<LocalDate> selectReleaseDate = MapBindings.select(media, "releaseDate");

      {
        bind(selectReleaseDate);
      }

      @Override
      protected String computeValue() {
        return selectReleaseDate.get() == null ? "" : DATE_TIME_FORMATTER.format(selectReleaseDate.get());
      }
    };
  }

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
