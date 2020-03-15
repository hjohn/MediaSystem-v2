package hs.mediasystem.ui.api.player;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;

import org.reactfx.value.Val;

public interface PlayerPresentation {
  void play(String uri, long positionInMillis);

  void stop();

  void dispose();

  void showSubtitle(Path path);

  /**
   * Returns the length of the stream in milliseconds.
   *
   * @return the length of the stream in milliseconds
   */
  Val<Long> lengthProperty();

  /**
   * Returns the position of the stream in milliseconds.
   *
   * @return the position of the stream in milliseconds
   */
  Property<Long> positionProperty();

  /**
   * Returns the current volume in percent.
   *
   * @return the current volume in percent
   */
  Property<Long> volumeProperty();

  BooleanProperty mutedProperty();

  BooleanProperty pausedProperty();

  /**
   * Returns the current subtitle delay in milliseconds.
   *
   * @return the current subtitle delay in milliseconds
   */
  Property<Long> subtitleDelayProperty();

  /**
   * Returns the current subtitle.  Will return a Subtitle.DISABLED when not showing any
   * subtitle.
   *
   * @return the current subtitle
   */
  Property<Subtitle> subtitleProperty();
  ObservableList<Subtitle> subtitles();

  /**
   * Returns the current rate of playback as factor of normal speed.
   *
   * @return the current rate of playback
   */
  Property<Double> rateProperty();

  /**
   * Returns the audio delay, in milliseconds.
   *
   * @return the audio delay, in milliseconds
   */
  Property<Long> audioDelayProperty();

  Property<AudioTrack> audioTrackProperty();
  ObservableList<AudioTrack> audioTracks();

  /**
   * Returns the brightness as a float between 0 and 2, with 1 being normal.
   *
   * @return the brightness
   */
  Property<Double> brightnessProperty();

  Object getDisplayComponent();

  ObjectProperty<EventHandler<PlayerEvent>> onPlayerEvent();
}
