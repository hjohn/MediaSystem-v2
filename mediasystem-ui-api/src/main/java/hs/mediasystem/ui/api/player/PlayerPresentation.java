package hs.mediasystem.ui.api.player;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;

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
  LongProperty lengthProperty();

  /**
   * Returns the position of the stream in milliseconds.
   *
   * @return the position of the stream in milliseconds
   */
  LongProperty positionProperty();

  /**
   * Returns the current volume in percent.
   *
   * @return the current volume in percent
   */
  LongProperty volumeProperty();

  BooleanProperty mutedProperty();

  BooleanProperty pausedProperty();

  Property<SubtitlePresentation> subtitlePresentationProperty();

  /**
   * Returns the current rate of playback as factor of normal speed.
   *
   * @return the current rate of playback
   */
  DoubleProperty rateProperty();

  /**
   * Returns the audio delay, in milliseconds.
   *
   * @return the audio delay, in milliseconds
   */
  LongProperty audioDelayProperty();

  Property<AudioTrack> audioTrackProperty();
  ObservableList<AudioTrack> audioTracks();

  /**
   * Returns the brightness as a float between 0 and 2, with 1 being normal.
   *
   * @return the brightness
   */
  DoubleProperty brightnessProperty();

  Node getDisplayComponent();

  ObjectProperty<EventHandler<PlayerEvent>> onPlayerEvent();

  Property<StatOverlay> statOverlayProperty();
  ObservableList<StatOverlay> statOverlays();
}
