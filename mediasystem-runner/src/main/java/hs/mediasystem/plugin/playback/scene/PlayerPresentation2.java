package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.framework.actions.Expose;
import hs.mediasystem.framework.actions.Range;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;

public class PlayerPresentation2 {
  @Expose
  public final LongProperty position = new SimpleLongProperty() {
    @Override
    public void set(long newValue) {
      super.set(clamp(newValue, 0L, Long.MAX_VALUE /*player.getLength()*/));  //FIXME
    }
  };

  @Expose
  public final BooleanProperty paused = new SimpleBooleanProperty();

  @Expose
  public final BooleanProperty muted = new SimpleBooleanProperty();

  @Expose @Range(min = 0, max = 100, step = 5)
  public final IntegerProperty volume = new SimpleIntegerProperty() {
    @Override
    public void set(int newValue) {
      super.set(clamp(newValue, 0, 100));
    }
  };

  @Expose @Range(min = 0, max = 2, step = 0.01)
  public final FloatProperty brightness = new SimpleFloatProperty() {
    @Override
    public void set(float newValue) {
      super.set(clamp(newValue, 0.0f, 2.0f));
    }
  };

  @Expose @Range(min = 0.1, max = 4, step = 0.1)
  public final FloatProperty rate = new SimpleFloatProperty() {
    @Override
    public void set(float newValue) {
      super.set(clamp(newValue, 0.1f, 4.0f));
    }
  };

  @Expose @Range(min = -300 * 1000, max = 300 * 1000, step = 100)
  public final IntegerProperty subtitleDelay = new SimpleIntegerProperty() {
    @Override
    public void set(int newValue) {
      super.set(clamp(newValue, -300 * 1000, 300 * 1000));
    }
  };

  @Expose @Range(min = -10 * 1000, max = 10 * 1000, step = 100)
  public final IntegerProperty audioDelay = new SimpleIntegerProperty() {
    @Override
    public void set(int newValue) {
      super.set(clamp(newValue, -10 * 1000, 10 * 1000));
    }
  };

//  @Expose(values = "availableSubtitles", stringConverter = SubtitleStringConverter.class)
//  public final ObjectProperty<Subtitle> subtitle = new SimpleObjectProperty<>();
//  public final ObservableList<Subtitle> availableSubtitles;
//
//  @Expose(values = "availableAudioTracks", stringConverter = AudioTrackStringConverter.class)
//  public final ObjectProperty<AudioTrack> audioTrack = new SimpleObjectProperty<>();
//  public final ObservableList<AudioTrack> availableAudioTracks;

  private static int clamp(int value, int min, int max) {
    return value < min ? min :
           value > max ? max : value;
  }

  private static long clamp(long value, long min, long max) {
    return value < min ? min :
           value > max ? max : value;
  }

  private static float clamp(float value, float min, float max) {
    return value < min ? min :
           value > max ? max : value;
  }
}
