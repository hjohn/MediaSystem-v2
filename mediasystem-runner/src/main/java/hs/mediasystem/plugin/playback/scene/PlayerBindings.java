package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.domain.AudioTrack;
import hs.mediasystem.domain.PlayerPresentation;
import hs.mediasystem.domain.Subtitle;
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.javafx.StringBinding;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;

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

  public final LongBinding position;
  public final LongBinding length;
  public final IntegerBinding volume;
  public final FloatBinding rate;
  public final IntegerBinding audioDelay;
  public final IntegerBinding subtitleDelay;
  public final FloatBinding brightness;
  public final ObjectBinding<AudioTrack> audioTrack;
  public final ObjectBinding<Subtitle> subtitle;
  public final BooleanBinding muted;
  public final BooleanBinding paused;

  public PlayerBindings(final ObjectProperty<PlayerPresentation> player) {
    position = (LongBinding)Bindings.when(player.isNull()).then(0L).otherwise(Bindings.selectLong(player, "position"));
    length = (LongBinding)Bindings.when(player.isNull()).then(0L).otherwise(Bindings.selectLong(player, "length"));
    volume = (IntegerBinding)Bindings.when(player.isNull()).then(100).otherwise(Bindings.selectInteger(player, "volume"));
    rate = (FloatBinding)Bindings.when(player.isNull()).then(1.0f).otherwise(Bindings.selectFloat(player, "rate"));
    audioDelay = Bindings.selectInteger(player, "audioDelay");
    subtitleDelay = Bindings.selectInteger(player, "subtitleDelay");
    brightness = Bindings.selectFloat(player, "brightness");
    audioTrack = Bindings.select(player, "audioTrack");
    subtitle = Bindings.select(player, "subtitle");
    muted = Bindings.when(player.isNull()).then(false).otherwise(Bindings.selectBoolean(player, "muted"));
    paused = Bindings.when(player.isNull()).then(false).otherwise(Bindings.selectBoolean(player, "paused"));

    formattedVolume = new StringBinding(volume) {
      @Override
      protected String computeValue() {
        return String.format("%3d%%", volume.get());
      }
    };

    formattedRate = new StringBinding(rate) {
      @Override
      protected String computeValue() {
        return String.format("%4.1fx", rate.get());
      }
    };

    formattedAudioDelay = new StringBinding(audioDelay) {
      @Override
      protected String computeValue() {
        return String.format("%5.1fs", audioDelay.get() / 1000.0);
      }
    };

    formattedSubtitleDelay = new StringBinding(subtitleDelay) {
      @Override
      protected String computeValue() {
        return String.format("%5.1fs", subtitleDelay.get() / 1000.0);
      }
    };

    formattedBrightness = new StringBinding(brightness) {
      @Override
      protected String computeValue() {
        long value = Math.round((brightness.get() - 1.0) * 100);
        return value == 0 ? "0%" : String.format("%+3d%%", value);
      }
    };

    formattedAudioTrack = new StringBinding(audioTrack) {
      @Override
      protected String computeValue() {
        AudioTrack value = audioTrack.get();

        return value == null ? "" : value.getDescription();
      }
    };

    formattedSubtitle = new StringBinding(subtitle) {
      @Override
      protected String computeValue() {
        Subtitle value = subtitle.get();

        return value == null ? "" : value.getDescription();
      }
    };

    formattedPosition = new StringBinding(position) {
      @Override
      protected String computeValue() {
        return SizeFormatter.SECONDS_AS_POSITION.format(position.get() / 1000);
      }
    };

    formattedLength = new StringBinding(length) {
      @Override
      protected String computeValue() {
        return SizeFormatter.SECONDS_AS_POSITION.format(length.get() / 1000);
      }
    };
  }
}
