package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.stream.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.scanner.api.StreamID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import javax.inject.Inject;

import org.reactfx.Change;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.value.Var;

public class EpisodesPresentation extends AbstractPresentation {
  private static final String SYSTEM = "MediaSystem:Episode";
  private static final Comparator<Work> SEASON_ORDER = (r1, r2) -> {
    int s1 = ((Episode)r1.getDescriptor()).getSeasonNumber();
    int s2 = ((Episode)r2.getDescriptor()).getSeasonNumber();

    return Integer.compare(s1 <= 0 ? Integer.MAX_VALUE - 10 - s1 : s1, s2 == 0 ? Integer.MAX_VALUE - 10 - s2 : s2);
  };
  private static final Comparator<Work> EPISODE_ORDER = (r1, r2) -> {
    int e1 = ((Episode)r1.getDescriptor()).getNumber();
    int e2 = ((Episode)r2.getDescriptor()).getNumber();

    return Integer.compare(e1, e2);
  };

  @Inject private SettingsStore settingsStore;
  @Inject private StreamStateService streamStateService;

  private final List<Work> internalEpisodeItems = new ArrayList<>();

  public final List<Work> episodeItems = Collections.unmodifiableList(internalEpisodeItems);
  public final Var<Work> episodeItem = Var.newSimpleVar(null);

  public final Map<Integer, Var<SeasonWatchState>> seasonWatchStates = new HashMap<>();

  public EpisodesPresentation set(Work serieItem, List<Work> episodes) {
    episodes.stream()
      .sorted(SEASON_ORDER.thenComparing(EPISODE_ORDER))
      .forEachOrdered(internalEpisodeItems::add);

    String settingKey = "last-selected:" + serieItem.getId();

    episodeItem.addListener((obs, old, current) -> settingsStore.storeSetting(SYSTEM, settingKey, current.getId().toString()));

    String id = settingsStore.getSetting(SYSTEM, settingKey);

    outer:
    for(;;) {
      if(id != null) {
        for(int i = 0; i < episodeItems.size(); i++) {
          Work work = episodeItems.get(i);

          if(id.equals(work.getId().toString())) {
            episodeItem.setValue(work);
            break outer;
          }
        }
      }

      if(!episodeItems.isEmpty()) {
        episodeItem.setValue(episodeItems.get(0));
      }
      break;
    }

    ObservableSet<EventStream<Change<Boolean>>> set = FXCollections.observableSet(new HashSet<>());

    for(Work episodeItem : internalEpisodeItems) {
      episodeItem.getPrimaryStream().map(MediaStream::getId).map(streamStateService::watchedProperty).ifPresent(p -> set.add(EventStreams.changesOf(p)));
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

    for(Work episodeItem : internalEpisodeItems) {
      StreamID streamId = episodeItem.getPrimaryStream().map(MediaStream::getId).orElse(null);
      boolean missing = streamId == null;
      boolean watched = streamId == null ? false : streamStateService.isWatched(streamId);

      int seasonNumber = ((Episode)episodeItem.getDescriptor()).getSeasonNumber();

      missingCounts.merge(seasonNumber, missing ? 1 : 0, (a, b) -> a + b);
      watchCounts.merge(seasonNumber, watched ? 1 : 0, (a, b) -> a + b);
      totalCounts.merge(seasonNumber, 1, (a, b) -> a + b);
    }

    for(Integer seasonNumber : totalCounts.keySet()) {
      seasonWatchStates.computeIfAbsent(seasonNumber, k -> Var.newSimpleVar(null))
        .setValue(new SeasonWatchState(totalCounts.get(seasonNumber), missingCounts.get(seasonNumber), watchCounts.get(seasonNumber)));
    }
  }
}
