package hs.mediasystem.domain;

import hs.mediasystem.framework.actions.Expose;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;

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
  long getLength();
  ReadOnlyLongProperty lengthProperty();

  /**
   * Returns the position of the stream in milliseconds.
   *
   * @return the position of the stream in milliseconds
   */
  long getPosition();
  void setPosition(long position);
  @Expose(name = "position")
  LongProperty positionProperty();

  /**
   * Returns the current volume in percent.
   *
   * @return the current volume in percent
   */
  int getVolume();
  void setVolume(int volume);
  @Expose(name = "volume")
  IntegerProperty volumeProperty();

  boolean isMuted();
  void setMuted(boolean mute);
  @Expose(name = "muted")
  BooleanProperty mutedProperty();

  boolean isPaused();
  void setPaused(boolean paused);
  @Expose(name = "paused")
  BooleanProperty pausedProperty();

  /**
   * Returns the current subtitle delay in milliseconds.
   *
   * @return the current subtitle delay in milliseconds
   */
  int getSubtitleDelay();
  void setSubtitleDelay(int delay);
  @Expose(name = "subtitleDelay")
  IntegerProperty subtitleDelayProperty();

  /**
   * Returns a list of Subtitles.  This list always includes as the first element
   * Subtitle.DISABLED.  This list is therefore never empty.
   *
   * @return a list of Subtitles
   */
  ObservableList<Subtitle> getSubtitles();

  /**
   * Returns the current subtitle.  Will return a Subtitle.DISABLED when not showing any
   * subtitle.
   *
   * @return the current subtitle
   */
  Subtitle getSubtitle();
  void setSubtitle(Subtitle subtitle);
  @Expose(name = "subtitle", values = "subtitles") // TODO we need this? : , stringConverter = SubtitleStringConverter.class)
  ObjectProperty<Subtitle> subtitleProperty();

  /**
   * Returns the current rate of playback as factor of normal speed.
   *
   * @return the current rate of playback
   */
  float getRate();
  void setRate(float rate);
  FloatProperty rateProperty();

  /**
   * Returns the audio delay, in milliseconds.
   *
   * @return the audio delay, in milliseconds
   */
  int getAudioDelay();
  void setAudioDelay(int rate);
  IntegerProperty audioDelayProperty();

  AudioTrack getAudioTrack();
  void setAudioTrack(AudioTrack audioTrack);
  ObjectProperty<AudioTrack> audioTrackProperty();

  /**
   * Returns the brightness as a float between 0 and 2, with 1 being normal.
   *
   * @return the brightness
   */
  float getBrightness();
  void setBrightness(float brightness);
  FloatProperty brightnessProperty();

  ObservableList<AudioTrack> getAudioTracks();

  Object getDisplayComponent();

  ObjectProperty<EventHandler<PlayerEvent>> onPlayerEvent();
}
