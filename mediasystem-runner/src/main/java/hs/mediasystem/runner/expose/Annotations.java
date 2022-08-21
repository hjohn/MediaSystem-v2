package hs.mediasystem.runner.expose;

import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.GridViewPresentation;
import hs.mediasystem.plugin.library.scene.grid.WorkCellPresentation;
import hs.mediasystem.plugin.library.scene.overview.EpisodePresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.ProductionPresentation;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.root.RootPresentation;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.ui.api.player.AudioTrack;
import hs.mediasystem.ui.api.player.PlayerPresentation;
import hs.mediasystem.ui.api.player.StatOverlay;
import hs.mediasystem.ui.api.player.Subtitle;
import hs.mediasystem.ui.api.player.SubtitlePresentation;
import hs.mediasystem.util.expose.Expose;

import javafx.beans.property.SimpleLongProperty;

public class Annotations {

  public static void initialize() {
    Expose.action(Navigable::navigateBack)
      .of(Navigable.class)
      .as("navigateBack");

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

    Expose.nodeProperty((PlayerPresentation p) -> p.subtitlePresentationProperty())
      .of(PlayerPresentation.class)
      .provides(SubtitlePresentation.class)
      .as("subtitlePresentation");

    Expose.longProperty(PlayerPresentation::positionProperty)
      .of(PlayerPresentation.class)
      .range((PlayerPresentation p) -> new SimpleLongProperty(0), (PlayerPresentation p) -> p.lengthProperty(), 3)
      .as("position");

    Expose.longProperty(PlayerPresentation::volumeProperty)
      .of(PlayerPresentation.class)
      .range(0, 100, 5)
      .as("volume");

    Expose.booleanProperty(PlayerPresentation::mutedProperty)
      .of(PlayerPresentation.class)
      .as("muted");

    Expose.booleanProperty(PlayerPresentation::pausedProperty)
      .of(PlayerPresentation.class)
      .as("paused");

    Expose.longProperty(SubtitlePresentation::subtitleDelayProperty)
      .of(SubtitlePresentation.class)
      .range(-60000, 60000, 100)
      .format(v -> v == 0 ? "None" :
            v % 1000 == 0 ? String.format("%+d s", v / 1000) : String.format("%+.1f s", v / 1000.0))
      .as("subtitleDelay");

    Expose.doubleProperty(PlayerPresentation::rateProperty)
      .of(PlayerPresentation.class)
      .range(0.1, 1.9, 0.1)
      .format(v -> v.floatValue() == 1.0f ? "Standard" : String.format("%+.0f%%", (v.floatValue() - 1) * 100))
      .as("rate");

    Expose.doubleProperty(PlayerPresentation::brightnessProperty)
      .of(PlayerPresentation.class)
      .range(0.0, 2.0, 0.01)
      .format(v -> v.floatValue() == 1.0f ? "Standard" : String.format("%+.0f%%", (v.floatValue() - 1) * 100))
      .as("brightness");

    Expose.listProperty(SubtitlePresentation::subtitleProperty)
      .of(SubtitlePresentation.class)
      .allowedValues(p -> p.subtitles())
      .format(Subtitle::getDescription)
      .as("subtitle");

    Expose.listProperty(PlayerPresentation::audioTrackProperty)
      .of(PlayerPresentation.class)
      .allowedValues(p -> p.audioTracks())
      .format(AudioTrack::getDescription)
      .as("audioTrack");

    Expose.listProperty(PlayerPresentation::statOverlayProperty)
      .of(PlayerPresentation.class)
      .allowedValues(p -> p.statOverlays())
      .format(StatOverlay::getDescription)
      .as("statOverlay");

    Expose.listProperty((GridViewPresentation<Object, Object> p) -> p.sortOrder)
      .of(GridViewPresentation.class)
      .allowedValues(p -> p.availableSortOrders)
      .format(f -> ResourceManager.getText(GridViewPresentationFactory.class, "sort-order", f.resourceKey))
      .as("sortOrder");

    Expose.listProperty((GridViewPresentation<Object, Object> p) -> p.filter)
      .of(GridViewPresentation.class)
      .allowedValues(p -> p.availableFilters)
      .format(f -> ResourceManager.getText(GridViewPresentationFactory.class, "filter", f.resourceKey))
      .as("filter");

    Expose.listProperty((GridViewPresentation<Object, Object> p) -> p.stateFilter)
      .of(GridViewPresentation.class)
      .allowedValues(p -> p.availableStateFilters)
      .format(f -> ResourceManager.getText(GridViewPresentationFactory.class, "stateFilter", f.resourceKey))
      .as("stateFilter");

    Expose.listProperty((GridViewPresentation<Object, Object> p) -> p.grouping)
      .of(GridViewPresentation.class)
      .allowedValues(p -> p.availableGroupings)
      .format(f -> ResourceManager.getText(GridViewPresentationFactory.class, "grouping", f.getClass().getSimpleName()))
      .as("grouping");

    Expose.booleanProperty(WorkCellPresentation::watchedProperty)
      .of(WorkCellPresentation.class)
      .as("watched");

    Expose.indirectAction(WorkCellPresentation::reidentify)
      .of(WorkCellPresentation.class)
      .as("reidentify");

    Expose.indirectAction(WorkCellPresentation::showInfo)
      .of(WorkCellPresentation.class)
      .as("showInfo");

    // Debug actions

    Expose.action(RootPresentation::debugOnce)
      .of(RootPresentation.class)
      .as("debugOnce");
  }
}
