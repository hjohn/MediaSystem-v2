package hs.mediasystem.domain;

import hs.mediasystem.framework.actions.controls.DecimalControl;
import hs.mediasystem.framework.actions.controls.IntegerControl;
import hs.mediasystem.framework.actions.controls.ListControl;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
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
  IntegerControl positionControl();

  /**
   * Returns the current volume in percent.
   *
   * @return the current volume in percent
   */
  IntegerControl volumeControl();

  BooleanProperty mutedProperty();

  BooleanProperty pausedProperty();

  /**
   * Returns the current subtitle delay in milliseconds.
   *
   * @return the current subtitle delay in milliseconds
   */
  IntegerControl subtitleDelayControl();

  /**
   * Returns the current subtitle.  Will return a Subtitle.DISABLED when not showing any
   * subtitle.
   *
   * @return the current subtitle
   */
  ListControl<Subtitle> subtitleControl();

  /**
   * Returns the current rate of playback as factor of normal speed.
   *
   * @return the current rate of playback
   */
  DecimalControl rateControl();

  /**
   * Returns the audio delay, in milliseconds.
   *
   * @return the audio delay, in milliseconds
   */
  IntegerControl audioDelayControl();

  ListControl<AudioTrack> audioTrackControl();

  /**
   * Returns the brightness as a float between 0 and 2, with 1 being normal.
   *
   * @return the brightness
   */
  DecimalControl brightnessControl();

  Object getDisplayComponent();

  ObjectProperty<EventHandler<PlayerEvent>> onPlayerEvent();
}
