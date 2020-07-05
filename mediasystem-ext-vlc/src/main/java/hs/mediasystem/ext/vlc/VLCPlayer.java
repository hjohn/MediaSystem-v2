package hs.mediasystem.ext.vlc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactfx.Change;
import org.reactfx.EventStreams;
import org.reactfx.SuspendableEventStream;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

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
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurfaceFactory;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.TrackDescription;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.WindowsVideoSurfaceAdapter;

public class VLCPlayer implements PlayerPresentation {
  public enum Mode {
    CANVAS, WID
  }

  private final EmbeddedMediaPlayer mediaPlayer;
  private final ImageView canvas;
  private final StackPane displayComponent;
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

    this.mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
    
    if(mode == Mode.WID) {
      this.canvas = null;
      this.displayComponent = null;

      this.mediaPlayer.videoSurface().set(new DeferredComponentIdVideoSurface(new WindowsVideoSurfaceAdapter()) {
        @Override
        protected long getComponentId() {
          return supplier.getWindowId();
        }
      });
    }
    else {
      this.canvas = new ImageView();
      this.displayComponent = new StackPane(canvas);
      
//      final ResizableWritableImageView canvas = new ResizableWritableImageView(16, 16);  // FIXME can be simplied to be only resizable?
            
//      pane.setAlignment(Pos.CENTER);
      
      canvas.setPreserveRatio(true);
      canvas.fitWidthProperty().bind(displayComponent.widthProperty());
      canvas.fitHeightProperty().bind(displayComponent.heightProperty());
     
      this.mediaPlayer.videoSurface().set(ImageViewVideoSurfaceFactory.videoSurfaceForImageView(canvas));
    }

    SuspendableEventStream<Change<Long>> positionChanges = EventStreams.changesOf(position).suppressible();

    positionChanges.observe(c -> mediaPlayer.controls().setTime(c.getNewValue().longValue()));  // Basically, only called when updated externally, not by player

    mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
      private final AtomicInteger ignoreFinish = new AtomicInteger();
      private boolean videoAdjusted;

      @Override
      public void timeChanged(final MediaPlayer mediaPlayer, final long newTime) {
        Platform.runLater(() -> positionChanges.suspendWhile(() -> position.setValue(newTime)));

        final int currentSubtitleId = mediaPlayer.subpictures().track();

        if(currentSubtitleId > 0 && currentSubtitleId != getSubtitleInternal().getId()) {
          System.out.println("[INFO] VLCPlayer: Subtitle changed externally to " + currentSubtitleId + ", updating to match");

          Platform.runLater(() -> {
            updateSubtitles();
            subtitle.setValue(getSubtitleInternal());
          });
        }

        if(!videoAdjusted) {
          mediaPlayer.video().setAdjustVideo(true);
          videoAdjusted = true;
        }
      }

      @Override
      public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
        Platform.runLater(() -> length.setValue(newLength));
      }

//      @Override
//      public void newMedia(MediaPlayer mediaPlayer) {
//        System.out.println("[FINE] VLCPlayer: Event[newMedia]");
//        updateSubtitles();
//        videoAdjusted = false;
//
//        // mediaPlayer.play();
//      }

      @Override
      public void mediaChanged(MediaPlayer mediaPlayer, MediaRef mediaRef) {
        System.out.println("[FINE] VLCPlayer: Event[mediaChanged]: " + mediaRef);
        updateSubtitles();
        videoAdjusted = false;
      }

//      @Override
//      public void mediaMetaChanged(MediaPlayer mediaPlayer, int metaType) {
//        // 12 = NowPlaying(?), 13 = Publisher(?), 15 = ArtworkURL(?)
//      }

      @Override
      public void paused(MediaPlayer mediaPlayer) {
        pausedProperty.update(true); 
      }
      
      @Override
      public void playing(MediaPlayer mediaPlayer) {
        pausedProperty.update(false);
      }
      
      @Override
      public void stopped(MediaPlayer mediaPlayer) {
        videoOutputStarted = false; 
      }
      
//      muted
      
//      @Override
//      public void mediaStateChanged(final MediaPlayer mediaPlayer, int newState) {
//        // IDLE/CLOSE=0, OPENING=1, BUFFERING=2, PLAYING=3, PAUSED=4, STOPPING=5, ENDED=6, ERROR=7
//        if(newState >= 5) {
//          videoOutputStarted = false;
//        }
//
//        System.out.println("[FINE] VLCPlayer: Event[mediaStateChanged]: " + newState);
//
//        pausedProperty.update(newState == 4);
//      }

      @Override
      public void mediaPlayerReady(MediaPlayer mediaPlayer) {
        System.out.println("[FINE] VLCPlayer: Event[mediaPlayerReady]");
        Platform.runLater(() -> {
          updateSubtitles();
          updateAudioTracks();
          System.out.println(">>> AudioTracks are now: " + audioTracks + ", setting to: " + getAudioTrackInternal());
          audioTrack.setValue(getAudioTrackInternal());
        });
      }

//      @Override
//      public void mediaSubItemAdded(MediaPlayer mediaPlayer, libvlc_media_t subItem) {
//        ignoreFinish.incrementAndGet();
//        int i = 1;
//
//        System.out.println("VLCPlayer: mediaSubItemAdded: " + subItem.toString());
//
//        for(TrackDescription desc : mediaPlayer.getTitleDescriptions()) {
//          System.out.println(i++ + " : " + desc.description());
//        }
//      }

      @Override
      public void videoOutput(final MediaPlayer mediaPlayer, int newCount) {
        System.out.println("VLCPlayer: videoOutput");

        Platform.runLater(() -> {
          mediaPlayer.audio().setVolume(volume.getValue().intValue());
          mediaPlayer.audio().setMute(mutedProperty.get());
        });

        videoOutputStarted = true;
      }

      @Override
      public void finished(MediaPlayer mediaPlayer) {
//        if(ignoreFinish.get() == 0) {
          videoOutputStarted = false;
          System.out.println("VLCPlayer: Finished");
          Events.dispatchEvent(onPlayerEvent, new PlayerEvent(Type.FINISHED), null);
//        }
//        else {
//          int index = mediaPlayer.subItemIndex();
//
//          List<String> subItems = mediaPlayer.subItems();
//
//          if(index>= 0 && index < subItems.size()) {
//            System.out.println("Finished: " + subItems.get(index));
//          }
//          else {
//            index = 0;
//          }
//
//          ignoreFinish.decrementAndGet();
//          System.out.println("VLCPlayer: Adding more media");
//          // mediaPlayer.playMedia(subItems.get(index));
//        }
      }
    });

    volume.addListener((obs, old, value) -> mediaPlayer.audio().setVolume(value.intValue()));
    audioDelay.addListener((obs, old, value) -> mediaPlayer.audio().setDelay(value * 1000));
    subtitleDelay.addListener((obs, old, value) -> mediaPlayer.subpictures().setDelay(-value * 1000));
    rate.addListener((obs, old, value) -> mediaPlayer.controls().setRate(value.floatValue()));
    brightness.addListener((obs, old, value) -> mediaPlayer.video().setBrightness(value.floatValue()));

    mutedProperty = new BeanBooleanProperty(new Accessor<Boolean>() {
      private boolean cachedMuteStatus = false;

      @Override
      public Boolean read() {
        if(videoOutputStarted) {
          cachedMuteStatus = mediaPlayer.audio().isMute();
        }

        return cachedMuteStatus;
      }

      @Override
      public void write(Boolean value) {
        mediaPlayer.audio().setMute(value);
      }
    });
    pausedProperty = new BeanBooleanProperty(new Accessor<Boolean>() {
      @Override
      public Boolean read() {
        return false;
      }

      @Override
      public void write(Boolean value) {
        mediaPlayer.controls().setPause(value);
      }
    });

    subtitle.addListener((obs, old, value) -> mediaPlayer.subpictures().setTrack(value.getId()));
    audioTrack.addListener((obs, old, value) -> {
      if(value != null) {
        mediaPlayer.audio().setTrack(value.getId());
      }
    });
  }

  public AudioTrack getAudioTrackInternal() {
    int index = mediaPlayer.audio().track();

    if(index == -1 || index >= audioTracks.size()) {
      return AudioTrack.NO_AUDIO_TRACK;
    }

    return audioTracks.get(index);
  }

  public void setAudioTrackInternal(AudioTrack audioTrack) {
    mediaPlayer.audio().setTrack(audioTrack.getId());
  }

  public Subtitle getSubtitleInternal() {
    int id = mediaPlayer.subpictures().track();

    for(Subtitle subtitle : subtitles) {
      if(subtitle.getId() == id) {
        return subtitle;
      }
    }

    return Subtitle.DISABLED;
  }

  public void setSubtitleInternal(Subtitle subtitle) {
    mediaPlayer.subpictures().setTrack(subtitle.getId());
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

//    if(canvas != null) {  FIXME
//      canvas.clear();
//    }

    position.setValue(0L);
    audioDelay.setValue(mediaPlayer.audio().delay() / 1000);
    audioTrack.setValue(getAudioTrackInternal());
    brightness.setValue((double)mediaPlayer.video().brightness());
    rate.setValue(1.0); // FIXME (double)mediaPlayer.getRate());
    subtitle.setValue(getSubtitleInternal());
    subtitleDelay.setValue(-mediaPlayer.subpictures().delay() / 1000);

    List<String> arguments = new ArrayList<>();

    if(positionInMillis > 0) {
      arguments.add("start-time=" + positionInMillis / 1000);
    }

    mediaPlayer.controls().setRepeat(false);
//    mediaPlayer.setPlaySubItems(true); FIXME
    mediaPlayer.media().play(uri, arguments.toArray(new String[arguments.size()]));

    System.out.println("[FINE] Playing: " + uri);
  }

  @Override
  public void stop() {
    mediaPlayer.controls().stop();
  }

  @Override
  public void dispose() {
    mediaPlayer.release();
  }

  @Override
  public Node getDisplayComponent() {
    return displayComponent;
  }

  @Override
  public void showSubtitle(Path path) {
    System.out.println("[INFO] VLCPlayer.showSubtitle: path = " + path.toString());
    mediaPlayer.subpictures().setSubTitleFile(path.toString());
  }

  private void updateSubtitles() {
    if(subtitles.size() > 1) {
      subtitles.subList(1, subtitles.size()).clear();
    }

    for(TrackDescription spuDescription : mediaPlayer.subpictures().trackDescriptions()) {
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
    for(TrackDescription description : mediaPlayer.audio().trackDescriptions()) {
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
