package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.ui.api.player.AudioTrack;
import hs.mediasystem.ui.api.player.PlayerPresentation;
import hs.mediasystem.ui.api.player.Subtitle;
import hs.mediasystem.ui.api.player.SubtitlePresentation;
import hs.mediasystem.util.SizeFormatter;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;

public class PlayerBindings {
  public final ObservableValue<String> formattedVolume;
  public final StringBinding formattedRate;
  public final StringBinding formattedAudioDelay;
  public final StringBinding formattedSubtitleDelay;
  public final StringBinding formattedBrightness;
  public final StringBinding formattedAudioTrack;
  public final StringBinding formattedSubtitle;
  public final StringBinding formattedPosition;
  public final StringBinding formattedLength;

  public final ObservableValue<Double> rate;
  public final ObservableValue<Long> position;
  public final ObservableValue<Long> length;
  public final ObservableValue<Long> volume;
  public final ObservableValue<Long> audioDelay;
  public final ObservableValue<Long> subtitleDelay;
  public final ObservableValue<Double> brightness;
  public final ObservableValue<AudioTrack> audioTrack;
  public final ObservableValue<Subtitle> subtitle;
  public final BooleanBinding muted;
  public final BooleanBinding paused;

  public PlayerBindings(final ObjectProperty<PlayerPresentation> player) {
    position = player.flatMap(PlayerPresentation::positionProperty).map(Number::longValue).orElse(0L);
    length = player.flatMap(PlayerPresentation::lengthProperty).map(Number::longValue).orElse(1000L);
    volume = player.flatMap(PlayerPresentation::volumeProperty).map(Number::longValue).orElse(100L);
    audioDelay = player.flatMap(PlayerPresentation::audioDelayProperty).map(Number::longValue).orElse(0L);
    subtitleDelay = player.flatMap(PlayerPresentation::subtitlePresentationProperty).flatMap(SubtitlePresentation::subtitleDelayProperty).map(Number::longValue).orElse(0L);
    rate = player.flatMap(PlayerPresentation::rateProperty).map(Number::doubleValue).orElse(1.0);
    brightness = player.flatMap(PlayerPresentation::brightnessProperty).map(Number::doubleValue).orElse(1.0);
    audioTrack = player.flatMap(PlayerPresentation::audioTrackProperty);
    subtitle = player.flatMap(PlayerPresentation::subtitlePresentationProperty).flatMap(SubtitlePresentation::subtitleProperty);
    muted = Bindings.when(player.isNull()).then(false).otherwise(Bindings.selectBoolean(player, "muted"));
    paused = Bindings.when(player.isNull()).then(false).otherwise(Bindings.selectBoolean(player, "paused"));

    formattedVolume = volume.map(v -> String.format("%3d%%", v));

    formattedRate = new StringBinding(rate) {
      @Override
      protected String computeValue() {
        return String.format("%4.1fx", rate.getValue().floatValue());
      }
    };

    formattedAudioDelay = new StringBinding(audioDelay) {
      @Override
      protected String computeValue() {
        return String.format("%5.1fs", audioDelay.getValue() / 1000.0);
      }
    };

    formattedSubtitleDelay = new StringBinding(subtitleDelay) {
      @Override
      protected String computeValue() {
        return String.format("%5.1fs", subtitleDelay.getValue() / 1000.0);
      }
    };

    formattedBrightness = new StringBinding(brightness) {
      @Override
      protected String computeValue() {
        long value = Math.round((brightness.getValue() - 1.0) * 100);
        return value == 0 ? "0%" : String.format("%+3d%%", value);
      }
    };

    formattedAudioTrack = new StringBinding(audioTrack) {
      @Override
      protected String computeValue() {
        AudioTrack value = audioTrack.getValue();

        return value == null ? "" : value.getDescription();
      }
    };

    formattedSubtitle = new StringBinding(subtitle) {
      @Override
      protected String computeValue() {
        Subtitle value = subtitle.getValue();

        return value == null ? "" : value.getDescription();
      }
    };

    formattedPosition = new StringBinding(position) {
      @Override
      protected String computeValue() {
        return SizeFormatter.SECONDS_AS_POSITION.format(position.getValue() / 1000);
      }
    };

    formattedLength = new StringBinding(length) {
      @Override
      protected String computeValue() {
        return SizeFormatter.SECONDS_AS_POSITION.format(length.getValue() / 1000);
      }
    };
  }

  static abstract class StringBinding extends javafx.beans.binding.StringBinding {
    public StringBinding(Observable... observables) {
      bind(observables);
    }
  }
}
