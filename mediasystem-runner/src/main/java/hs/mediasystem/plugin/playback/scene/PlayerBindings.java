package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.ui.api.player.AudioTrack;
import hs.mediasystem.ui.api.player.PlayerPresentation;
import hs.mediasystem.ui.api.player.Subtitle;
import hs.mediasystem.util.SizeFormatter;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;

import org.reactfx.value.Val;

public class PlayerBindings {
  public final StringBinding formattedVolume;
  public final StringBinding formattedRate;
  public final StringBinding formattedAudioDelay;
  public final StringBinding formattedSubtitleDelay;
  public final StringBinding formattedBrightness;
  public final StringBinding formattedAudioTrack;
  public final StringBinding formattedSubtitle;
  public final StringBinding formattedPosition;
  public final StringBinding formattedLength;

  public final Val<Double> rate;
  public final Val<Long> position;
  public final Val<Long> length;
  public final Val<Long> volume;
  public final Val<Long> audioDelay;
  public final Val<Long> subtitleDelay;
  public final Val<Double> brightness;
  public final Val<AudioTrack> audioTrack;
  public final Val<Subtitle> subtitle;
  public final BooleanBinding muted;
  public final BooleanBinding paused;

  public PlayerBindings(final ObjectProperty<PlayerPresentation> player) {
    position = Val.flatMap(player, PlayerPresentation::positionProperty).orElseConst(0L);
    length = Val.flatMap(player, PlayerPresentation::lengthProperty).orElseConst(1000L);
    volume = Val.flatMap(player, PlayerPresentation::volumeProperty).orElseConst(100L);
    rate = Val.flatMap(player, PlayerPresentation::rateProperty).orElseConst(1.0);
    audioDelay = Val.flatMap(player, PlayerPresentation::audioDelayProperty).orElseConst(0L);
    subtitleDelay = Val.flatMap(player, PlayerPresentation::subtitleDelayProperty).orElseConst(0L);
    brightness = Val.flatMap(player, PlayerPresentation::brightnessProperty).orElseConst(1.0);
    audioTrack = Val.flatMap(player, PlayerPresentation::audioTrackProperty);
    subtitle = Val.flatMap(player, PlayerPresentation::subtitleProperty);
    muted = Bindings.when(player.isNull()).then(false).otherwise(Bindings.selectBoolean(player, "muted"));
    paused = Bindings.when(player.isNull()).then(false).otherwise(Bindings.selectBoolean(player, "paused"));

    formattedVolume = new StringBinding(volume) {
      @Override
      protected String computeValue() {
        return String.format("%3d%%", volume.getValue());
      }
    };

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
