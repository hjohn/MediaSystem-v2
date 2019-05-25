package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.mediamanager.LocalSerie;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.scanner.api.BasicStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import javax.inject.Inject;

import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.value.Var;

public class EpisodesPresentation extends AbstractPresentation {
  private static final String SYSTEM = "MediaSystem:Episode";

  @Inject private SettingsStore settingsStore;
  @Inject private MediaItem.Factory mediaItemFactory;
  @Inject private StreamStateService streamStateService;

  private final List<MediaItem<Episode>> internalEpisodeItems = new ArrayList<>();

  public final List<MediaItem<Episode>> episodeItems = Collections.unmodifiableList(internalEpisodeItems);
  public final Var<MediaItem<Episode>> episodeItem = Var.newSimpleVar(null);

  public final Map<Integer, Var<SeasonWatchState>> seasonWatchStates = new HashMap<>();

  public EpisodesPresentation set(MediaItem<Serie> serieItem) {
    Serie serieDescriptor = serieItem.getData();

    serieDescriptor.getSeasons().stream()
      .sorted((s1, s2) -> Integer.compare(s1.getNumber() <= 0 ? Integer.MAX_VALUE - 10 - s1.getNumber() : s1.getNumber(), s2.getNumber() == 0 ? Integer.MAX_VALUE - 10 - s2.getNumber() : s2.getNumber()))
      .flatMap(s -> s.getEpisodes().stream())
      .map(e -> mediaItemFactory.create(e, serieItem))
      .forEachOrdered(internalEpisodeItems::add);

    if(serieDescriptor instanceof LocalSerie) {
      LocalSerie localSerie = (LocalSerie)serieDescriptor;

      localSerie.getExtras().stream()
        .map(e -> mediaItemFactory.create(e, serieItem))
        .forEachOrdered(internalEpisodeItems::add);
    }

    String settingKey = "last-selected:" + serieItem.getId();

    episodeItem.addListener((obs, old, current) -> settingsStore.storeSetting(SYSTEM, settingKey, current.getId()));

    String id = settingsStore.getSetting(SYSTEM, settingKey);

    outer:
    for(;;) {
      if(id != null) {
        for(int i = 0; i < episodeItems.size(); i++) {
          MediaItem<Episode> mediaItem = episodeItems.get(i);

          if(id.equals(mediaItem.getId())) {
            episodeItem.setValue(mediaItem);
            break outer;
          }
        }
      }

      if(!episodeItems.isEmpty()) {
        episodeItem.setValue(episodeItems.get(0));
      }
      break;
    }

    ObservableSet<EventStream<?>> set = FXCollections.observableSet();

    for(MediaItem<Episode> episodeItem : internalEpisodeItems) {
      set.add(EventStreams.changesOf(episodeItem.missing));
      set.add(EventStreams.changesOf(episodeItem.watched));
    }

    EventStreams.merge(set)
      .withDefaultEvent(null)
      .observe(e -> updateSeasonWatchState());

    return this;
  }

  private void updateSeasonWatchState() {
    Map<Integer, Integer> missingCounts = new HashMap<>();
    Map<Integer, Integer> watchCounts = new HashMap<>();
    Map<Integer, Integer> totalCounts = new HashMap<>();

    for(MediaItem<Episode> episodeItem : internalEpisodeItems) {
      BasicStream stream = episodeItem.getStream();
      boolean missing = stream == null;
      boolean watched = stream == null ? false : streamStateService.isWatched(stream.getId());

      missingCounts.merge(episodeItem.getData().getSeasonNumber(), missing ? 1 : 0, (a, b) -> a + b);
      watchCounts.merge(episodeItem.getData().getSeasonNumber(), watched ? 1 : 0, (a, b) -> a + b);
      totalCounts.merge(episodeItem.getData().getSeasonNumber(), 1, (a, b) -> a + b);
    }

    for(Integer seasonNumber : totalCounts.keySet()) {
      seasonWatchStates.computeIfAbsent(seasonNumber, k -> Var.newSimpleVar(null))
        .setValue(new SeasonWatchState(totalCounts.get(seasonNumber), missingCounts.get(seasonNumber), watchCounts.get(seasonNumber)));
    }
  }
}
