package hs.mediasystem.ext.vlc;

import com.sun.jna.Memory;

import hs.mediasystem.ext.vlc.util.Accessor;
import hs.mediasystem.ext.vlc.util.BeanBooleanProperty;
import hs.mediasystem.ui.api.player.AudioTrack;
import hs.mediasystem.ui.api.player.PlayerEvent;
import hs.mediasystem.ui.api.player.PlayerEvent.Type;
import hs.mediasystem.ui.api.player.PlayerPresentation;
import hs.mediasystem.ui.api.player.PlayerWindowIdSupplier;
import hs.mediasystem.ui.api.player.StatOverlay;
import hs.mediasystem.ui.api.player.Subtitle;
import hs.mediasystem.util.javafx.Events;
import hs.mediasystem.util.javafx.control.ResizableWritableImageView;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;

import org.reactfx.Change;
import org.reactfx.EventStreams;
import org.reactfx.SuspendableEventStream;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.TrackDescription;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallback;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.windows.WindowsVideoSurfaceAdapter;

public class VLCPlayer implements PlayerPresentation {
  public enum Mode {
    CANVAS, WID
  }

  private final MediaPlayer mediaPlayer;
  private final Object canvas;
  private final AtomicInteger frameNumber = new AtomicInteger();

  private volatile boolean videoOutputStarted;

  public VLCPlayer(Mode mode, PlayerWindowIdSupplier supplier, String... args) {
    List<String> arguments = new ArrayList<>(Arrays.asList(args));

    arguments.add("--no-plugins-cache");
    arguments.add("--no-snapshot-preview");
    arguments.add("--input-fast-seek");
    arguments.add("--no-video-title-show");
    arguments.add("--disable-screensaver");
    arguments.add("--network-caching");
    arguments.add("3000");
    arguments.add("--quiet");
    arguments.add("--quiet-synchro");
    arguments.add("--intf");
    arguments.add("dummy");

    MediaPlayerFactory factory = new MediaPlayerFactory(arguments);

    if(mode == Mode.WID) {
      EmbeddedMediaPlayer mp = factory.newEmbeddedMediaPlayer();

      mp.setVideoSurface(new DeferredComponentIdVideoSurface(new WindowsVideoSurfaceAdapter()) {
        @Override
        protected long getComponentId() {
          return supplier.getWindowId();
        }
      });

      this.canvas = null;
      this.mediaPlayer = mp;
    }
    else {
      final ResizableWritableImageView canvas = new ResizableWritableImageView(16, 16);
      final WritablePixelFormat<ByteBuffer> byteBgraInstance = PixelFormat.getByteBgraPreInstance();

      DirectMediaPlayer mp = factory.newDirectMediaPlayer(
        new BufferFormatCallback() {
          @Override
          public BufferFormat getBufferFormat(final int width, final int height) {
            System.out.println("[FINE] VLCPlayer: Buffer size changed to " + width + "x" + height);

            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                canvas.resize(width, height);
              }
            });

            return new RV32BufferFormat(width, height);
          }
        },
        new RenderCallback() {
          AtomicReference<ByteBuffer> currentByteBuffer = new AtomicReference<>();

          @Override
          public void display(DirectMediaPlayer mp, Memory[] memory, final BufferFormat bufferFormat) {
            final int renderFrameNumber = frameNumber.incrementAndGet();

            currentByteBuffer.set(memory[0].getByteBuffer(0, memory[0].size()));

            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                ByteBuffer byteBuffer = currentByteBuffer.get();
                int actualFrameNumber = frameNumber.get();

                if(renderFrameNumber == actualFrameNumber) {
                  canvas.getPixelWriter().setPixels(0, 0, bufferFormat.getWidth(), bufferFormat.getHeight(), byteBgraInstance, byteBuffer, bufferFormat.getPitches()[0]);
                }
                else {
                  System.out.println("[FINE] " + VLCPlayer.this.getClass().getSimpleName() + " - Skipped late frame " + renderFrameNumber + " (actual = " + actualFrameNumber + ")");
                }
              }
            });
          }
        }
      );

      this.canvas = canvas;
      this.mediaPlayer = mp;
    }

    SuspendableEventStream<Change<Long>> positionChanges = EventStreams.changesOf(position).suppressible();

    positionChanges.observe(c -> mediaPlayer.setTime(c.getNewValue().longValue()));  // Basically, only called when updated externally, not by player

    mediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
      private final AtomicInteger ignoreFinish = new AtomicInteger();
      private boolean videoAdjusted;

      @Override
      public void timeChanged(final MediaPlayer mediaPlayer, final long newTime) {
        Platform.runLater(() -> positionChanges.suspendWhile(() -> position.setValue(newTime)));

        final int currentSubtitleId = mediaPlayer.getSpu();

        if(currentSubtitleId > 0 && currentSubtitleId != getSubtitleInternal().getId()) {
          System.out.println("[INFO] VLCPlayer: Subtitle changed externally to " + currentSubtitleId + ", updating to match");

          Platform.runLater(() -> {
            updateSubtitles();
            subtitle.setValue(getSubtitleInternal());
          });
        }

        if(!videoAdjusted) {
          mediaPlayer.setAdjustVideo(true);
          videoAdjusted = true;
        }
      }

      @Override
      public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
        Platform.runLater(() -> length.setValue(newLength));
      }

      @Override
      public void newMedia(MediaPlayer mediaPlayer) {
        System.out.println("[FINE] VLCPlayer: Event[newMedia]");
        updateSubtitles();
        videoAdjusted = false;

        // mediaPlayer.play();
      }

      @Override
      public void mediaChanged(MediaPlayer mediaPlayer, libvlc_media_t media, String mrl) {
        System.out.println("[FINE] VLCPlayer: Event[mediaChanged] '" + mrl + "': " + media);
      }

      @Override
      public void mediaMetaChanged(MediaPlayer mediaPlayer, int metaType) {
        // 12 = NowPlaying(?), 13 = Publisher(?), 15 = ArtworkURL(?)
      }

      @Override
      public void mediaStateChanged(final MediaPlayer mediaPlayer, int newState) {
        // IDLE/CLOSE=0, OPENING=1, BUFFERING=2, PLAYING=3, PAUSED=4, STOPPING=5, ENDED=6, ERROR=7
        if(newState >= 5) {
          videoOutputStarted = false;
        }

        System.out.println("[FINE] VLCPlayer: Event[mediaStateChanged]: " + newState);

        pausedProperty.update(newState == 4);
      }

      @Override
      public void mediaParsedChanged(MediaPlayer mediaPlayer, int parsed) {
        System.out.println("[FINE] VLCPlayer: Event[mediaParsedChanged]: " + parsed);
        if(parsed == 1) {
          Platform.runLater(() -> {
            updateSubtitles();
            updateAudioTracks();
            System.out.println(">>> AudioTracks are now: " + audioTracks + ", setting to: " + getAudioTrackInternal());
            audioTrack.setValue(getAudioTrackInternal());
          });
        }
      }

      @Override
      public void mediaSubItemAdded(MediaPlayer mediaPlayer, libvlc_media_t subItem) {
        ignoreFinish.incrementAndGet();
        int i = 1;

        System.out.println("VLCPlayer: mediaSubItemAdded: " + subItem.toString());

        for(TrackDescription desc : mediaPlayer.getTitleDescriptions()) {
          System.out.println(i++ + " : " + desc.description());
        }
      }

      @Override
      public void videoOutput(final MediaPlayer mediaPlayer, int newCount) {
        System.out.println("VLCPlayer: videoOutput");

        Platform.runLater(() -> {
          mediaPlayer.setVolume(volume.getValue().intValue());
          mediaPlayer.mute(mutedProperty.get());
        });

        videoOutputStarted = true;
      }

      @Override
      public void finished(MediaPlayer mediaPlayer) {
        if(ignoreFinish.get() == 0) {
          videoOutputStarted = false;
          System.out.println("VLCPlayer: Finished");
          Events.dispatchEvent(onPlayerEvent, new PlayerEvent(Type.FINISHED), null);
        }
        else {
          int index = mediaPlayer.subItemIndex();

          List<String> subItems = mediaPlayer.subItems();

          if(index>= 0 && index < subItems.size()) {
            System.out.println("Finished: " + subItems.get(index));
          }
          else {
            index = 0;
          }

          ignoreFinish.decrementAndGet();
          System.out.println("VLCPlayer: Adding more media");
          // mediaPlayer.playMedia(subItems.get(index));
        }
      }
    });

    volume.addListener((obs, old, value) -> mediaPlayer.setVolume(value.intValue()));
    audioDelay.addListener((obs, old, value) -> mediaPlayer.setAudioDelay(value * 1000));
    subtitleDelay.addListener((obs, old, value) -> mediaPlayer.setSpuDelay(-value * 1000));
    rate.addListener((obs, old, value) -> mediaPlayer.setRate(value.floatValue()));
    brightness.addListener((obs, old, value) -> mediaPlayer.setBrightness(value.floatValue()));

    mutedProperty = new BeanBooleanProperty(new Accessor<Boolean>() {
      private boolean cachedMuteStatus = false;

      @Override
      public Boolean read() {
        if(videoOutputStarted) {
          cachedMuteStatus = mediaPlayer.isMute();
        }

        return cachedMuteStatus;
      }

      @Override
      public void write(Boolean value) {
        mediaPlayer.mute(value);
      }
    });
    pausedProperty = new BeanBooleanProperty(new Accessor<Boolean>() {
      @Override
      public Boolean read() {
        return false;
      }

      @Override
      public void write(Boolean value) {
        mediaPlayer.setPause(value);
      }
    });

    subtitle.addListener((obs, old, value) -> mediaPlayer.setSpu(value.getId()));
    audioTrack.addListener((obs, old, value) -> {
      if(value != null) {
        mediaPlayer.setAudioTrack(value.getId());
      }
    });
  }

  public AudioTrack getAudioTrackInternal() {
    int index = mediaPlayer.getAudioTrack();

    if(index == -1 || index >= audioTracks.size()) {
      return AudioTrack.NO_AUDIO_TRACK;
    }

    return audioTracks.get(index);
  }

  public void setAudioTrackInternal(AudioTrack audioTrack) {
    mediaPlayer.setAudioTrack(audioTrack.getId());
  }

  public Subtitle getSubtitleInternal() {
    int id = mediaPlayer.getSpu();

    for(Subtitle subtitle : subtitles) {
      if(subtitle.getId() == id) {
        return subtitle;
      }
    }

    return Subtitle.DISABLED;
  }

  public void setSubtitleInternal(Subtitle subtitle) {
    mediaPlayer.setSpu(subtitle.getId());
  }

  private final ObservableList<Subtitle> subtitles = FXCollections.observableArrayList(Subtitle.DISABLED);
  private final ObservableList<AudioTrack> audioTracks = FXCollections.observableArrayList(AudioTrack.NO_AUDIO_TRACK);

  @Override
  public Property<StatOverlay> statOverlayProperty() {
    return null;
  }

  @Override
  public ObservableList<StatOverlay> statOverlays() {
    return FXCollections.emptyObservableList();
  }

  @Override
  public void play(String uri, long positionInMillis) {
    frameNumber.set(0);

    if(canvas instanceof ResizableWritableImageView) {
      ResizableWritableImageView resizableWritableImageView = (ResizableWritableImageView)canvas;

      resizableWritableImageView.clear();
    }

    position.setValue(0L);
    audioDelay.setValue(mediaPlayer.getAudioDelay() / 1000);
    audioTrack.setValue(getAudioTrackInternal());
    brightness.setValue((double)mediaPlayer.getBrightness());
    rate.setValue((double)mediaPlayer.getRate());
    subtitle.setValue(getSubtitleInternal());
    subtitleDelay.setValue(-mediaPlayer.getSpuDelay() / 1000);

    List<String> arguments = new ArrayList<>();

    if(positionInMillis > 0) {
      arguments.add("start-time=" + positionInMillis / 1000);
    }

    mediaPlayer.setRepeat(false);
    mediaPlayer.setPlaySubItems(true);
    mediaPlayer.playMedia(uri, arguments.toArray(new String[arguments.size()]));

    System.out.println("[FINE] Playing: " + uri);
  }

  @Override
  public void stop() {
    mediaPlayer.stop();
  }

  @Override
  public void dispose() {
    mediaPlayer.release();
  }

  @Override
  public Object getDisplayComponent() {
    return canvas;
  }

  @Override
  public void showSubtitle(Path path) {
    System.out.println("[INFO] VLCPlayer.showSubtitle: path = " + path.toString());
    mediaPlayer.setSubTitleFile(path.toString());
  }

  private void updateSubtitles() {
    if(subtitles.size() > 1) {
      subtitles.subList(1, subtitles.size()).clear();
    }

    for(TrackDescription spuDescription : mediaPlayer.getSpuDescriptions()) {
      if(spuDescription.id() >= 0) {
        subtitles.add(new Subtitle(spuDescription.id(), spuDescription.description()));
      }
    }

    System.out.println("[FINE] VLCPlayer.updateSubtitles(), now available: " + subtitles);
  }

  private void updateAudioTracks() {
    Set<Integer> foundIds = new HashSet<>();

    /*
     * Update the AudioTracks observable list in place, with minimal disturbances:
     */

    next:
    for(TrackDescription description : mediaPlayer.getAudioDescriptions()) {
      foundIds.add(description.id());

      for(AudioTrack audioTrack : audioTracks) {
        if(audioTrack.getId() == description.id()) {
          continue next;
        }
      }

      audioTracks.add(new AudioTrack(description.id(), description.description()));
    }

    for(Iterator<AudioTrack> iterator = audioTracks.iterator(); iterator.hasNext();) {
      AudioTrack audioTrack = iterator.next();

      if(!foundIds.contains(audioTrack.getId())) {
        iterator.remove();
      }
    }

    System.out.println("[FINE] VLCPlayer.updateAudioTracks(), now available: " + audioTracks);
  }

  private final Var<Long> length = Var.newSimpleVar(null);
  @Override public Val<Long> lengthProperty() { return length.orElseConst(0L); }

  private final Property<Long> position = Var.newSimpleVar(0L);
  @Override public Property<Long> positionProperty() { return position; }

  private final Property<Long> volume = Var.newSimpleVar(100L);
  @Override public Property<Long> volumeProperty() { return volume; }

  private final Property<Long> audioDelay = Var.newSimpleVar(0L);
  @Override public Property<Long> audioDelayProperty() { return audioDelay; }

  private final Property<Double> rate = Var.newSimpleVar(1.0);
  @Override public Property<Double> rateProperty() { return rate; }

  private final Property<Double> brightness = Var.newSimpleVar(1.0);
  @Override public Property<Double> brightnessProperty() { return brightness; }

  private final Var<Subtitle> subtitle = Var.newSimpleVar(Subtitle.DISABLED);
  @Override public Property<Subtitle> subtitleProperty() { return subtitle; }
  @Override public ObservableList<Subtitle> subtitles() { return FXCollections.unmodifiableObservableList(subtitles); }

  private final Var<AudioTrack> audioTrack = Var.newSimpleVar(AudioTrack.NO_AUDIO_TRACK);
  @Override public Property<AudioTrack> audioTrackProperty() { return audioTrack; }
  @Override public ObservableList<AudioTrack> audioTracks() { return FXCollections.unmodifiableObservableList(audioTracks); }

  private final Property<Long> subtitleDelay = Var.newSimpleVar(0L);
  @Override public Property<Long> subtitleDelayProperty() { return subtitleDelay; }

  private final BooleanProperty mutedProperty;
  @Override public BooleanProperty mutedProperty() { return mutedProperty; }

  private final BeanBooleanProperty pausedProperty;
  @Override public BooleanProperty pausedProperty() { return pausedProperty; }

  private final ObjectProperty<EventHandler<PlayerEvent>> onPlayerEvent = new SimpleObjectProperty<>();
  @Override public ObjectProperty<EventHandler<PlayerEvent>> onPlayerEvent() { return onPlayerEvent; }
}
