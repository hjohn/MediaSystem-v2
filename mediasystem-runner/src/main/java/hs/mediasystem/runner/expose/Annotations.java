package hs.mediasystem.runner.expose;

import hs.mediasystem.domain.AudioTrack;
import hs.mediasystem.domain.PlayerPresentation;
import hs.mediasystem.domain.Subtitle;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.framework.expose.Expose;
import hs.mediasystem.plugin.library.scene.serie.EpisodePresentation;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.runner.root.RootPresentation;
import hs.mediasystem.runner.util.ResourceManager;

import org.reactfx.value.Val;

public class Annotations {

  public static void initialize() {
    Expose.action(EpisodePresentation::next)
      .of(EpisodePresentation.class)
      .as("next");

    Expose.action(EpisodePresentation::previous)
      .of(EpisodePresentation.class)
      .as("previous");

    Expose.action(ProductionPresentation::showInfo)
      .of(ProductionPresentation.class)
      .as("showInfo");

    Expose.booleanProperty(ProductionPresentation::watchedProperty)
      .of(ProductionPresentation.class)
      .as("watched");

    Expose.booleanProperty(ProductionPresentation::episodeWatchedProperty)
      .of(ProductionPresentation.class)
      .as("episodeWatched");

    Expose.booleanProperty(ProductionPresentation::seasonWatchedProperty)
      .of(ProductionPresentation.class)
      .asTriState(true)
      .as("seasonWatched");

    Expose.booleanProperty((PlaybackOverlayPresentation p) -> p.overlayVisible)
      .of(PlaybackOverlayPresentation.class)
      .as("overlayVisible");

    Expose.nodeProperty((PlaybackOverlayPresentation p) -> p.playerPresentation)
      .of(PlaybackOverlayPresentation.class)
      .provides(PlayerPresentation.class)
      .as("player");

    Expose.longProperty(PlayerPresentation::positionControl)
      .of(PlayerPresentation.class)
      .range((PlayerPresentation p) -> Val.constant(Long.valueOf(0)), (PlayerPresentation p) -> p.lengthProperty(), 3)
      .as("position");

    Expose.longProperty(PlayerPresentation::volumeControl)
      .of(PlayerPresentation.class)
      .range(0, 100, 5)
      .as("volume");

    Expose.booleanProperty(PlayerPresentation::mutedProperty)
      .of(PlayerPresentation.class)
      .as("muted");

    Expose.booleanProperty(PlayerPresentation::pausedProperty)
      .of(PlayerPresentation.class)
      .as("paused");

    Expose.longProperty(PlayerPresentation::subtitleDelayControl)
      .of(PlayerPresentation.class)
      .range(-60000, 60000, 100)
      .format(v -> v == 0 ? "None" :
            v % 1000 == 0 ? String.format("%+d s", v / 1000) : String.format("%+.1f s", v / 1000.0))
      .as("subtitleDelay");

    Expose.doubleProperty(PlayerPresentation::rateControl)
      .of(PlayerPresentation.class)
      .range(0.1, 1.9, 0.1)
      .format(v -> v.floatValue() == 1.0f ? "Standard" : String.format("%+.0f%%", (v.floatValue() - 1) * 100))
      .as("rate");

    Expose.doubleProperty(PlayerPresentation::brightnessControl)
      .of(PlayerPresentation.class)
      .range(0.0, 2.0, 0.01)
      .format(v -> v.floatValue() == 1.0f ? "Standard" : String.format("%+.0f%%", (v.floatValue() - 1) * 100))
      .as("brightness");

    Expose.listProperty(PlayerPresentation::subtitleControl)
      .of(PlayerPresentation.class)
      .allowedValues(p -> p.subtitleControl().getAllowedValues())
      .format(Subtitle::getDescription)
      .as("subtitle");

    Expose.listProperty(PlayerPresentation::audioTrackControl)
      .of(PlayerPresentation.class)
      .allowedValues(p -> p.audioTrackControl().getAllowedValues())
      .format(AudioTrack::getDescription)
      .as("audioTrack");

    Expose.listProperty((GridViewPresentation<MediaDescriptor> p) -> p.sortOrder)
      .of(GridViewPresentation.class)
      .allowedValues(p -> p.availableSortOrders)
      .format(f -> ResourceManager.getText(GridViewPresentation.class, "sort-order", f.resourceKey))
      .as("sortOrder");

    Expose.listProperty((GridViewPresentation<MediaDescriptor> p) -> p.filter)
      .of(GridViewPresentation.class)
      .allowedValues(p -> p.availableFilters)
      .format(f -> ResourceManager.getText(GridViewPresentation.class, "filter", f.resourceKey))
      .as("filter");

    Expose.listProperty((GridViewPresentation<MediaDescriptor> p) -> p.stateFilter)
      .of(GridViewPresentation.class)
      .allowedValues(p -> p.availableStateFilters)
      .format(f -> ResourceManager.getText(GridViewPresentation.class, "stateFilter", f.name().toLowerCase()))
      .as("stateFilter");

    Expose.listProperty((GridViewPresentation<MediaDescriptor> p) -> p.group)
      .of(GridViewPresentation.class)
      .allowedValues(p -> p.availableGroups)
      .format(f -> ResourceManager.getText(GridViewPresentation.class, "group", f.resourceKey))
      .as("group");

    Expose.booleanProperty(GridViewPresentation<?>::watchedProperty)
      .of(GridViewPresentation.class)
      .as("watched");

    Expose.action(GridViewPresentation<?>::reidentify)
      .of(GridViewPresentation.class)
      .as("reidentify");

    // Debug actions

    Expose.action(RootPresentation::debugOnce)
      .of(RootPresentation.class)
      .as("debugOnce");
  }
}
