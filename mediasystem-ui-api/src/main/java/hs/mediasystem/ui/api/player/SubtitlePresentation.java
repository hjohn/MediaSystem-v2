package hs.mediasystem.ui.api.player;

import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.collections.ObservableList;

public interface SubtitlePresentation {

  /**
   * Returns the current subtitle delay in milliseconds.
   *
   * @return the current subtitle delay in milliseconds
   */
  LongProperty subtitleDelayProperty();

  /**
   * Returns the current subtitle.  Will return a Subtitle.DISABLED when not showing any
   * subtitle.
   *
   * @return the current subtitle
   */
  Property<Subtitle> subtitleProperty();
  ObservableList<Subtitle> subtitles();
}
