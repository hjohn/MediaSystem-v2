package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.SettingsSource;
import hs.mediasystem.ui.api.domain.Work;

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
  private static final Comparator<Work> SEASON_ORDER = (w1, w2) -> {
    int s1 = toSeasonBarIndex(w1);
    int s2 = toSeasonBarIndex(w2);

    return Integer.compare(s1 <= 0 ? Integer.MAX_VALUE - 10 - s1 : s1, s2 <= 0 ? Integer.MAX_VALUE - 10 - s2 : s2);
  };
  private static final Comparator<Work> EPISODE_ORDER = (w1, w2) -> {
    int e1 = w1.getDetails().getSequence().map(Sequence::getNumber).orElse(0);
    int e2 = w2.getDetails().getSequence().map(Sequence::getNumber).orElse(0);

    return Integer.compare(e1, e2);
  };

  @Inject private SettingsClient settingsClient;

  private final List<Work> internalEpisodeItems = new ArrayList<>();

  public final List<Work> episodeItems = Collections.unmodifiableList(internalEpisodeItems);
  public final Var<Work> episodeItem = Var.newSimpleVar(null);

  public final Map<Integer, Var<SeasonWatchState>> seasonWatchStates = new HashMap<>();

  private SettingsSource settingsSource;

  public EpisodesPresentation set(Work serieItem, List<Work> episodes) {
    episodes.stream()
      .sorted(SEASON_ORDER.thenComparing(EPISODE_ORDER))
      .forEachOrdered(internalEpisodeItems::add);

    String settingKey = "last-selected:" + serieItem.getId();

    settingsSource = settingsClient.of(SYSTEM);

    episodeItem.addListener((obs, old, current) -> settingsSource.storeSetting(settingKey, current.getId().toString()));

    String id = settingsSource.getSetting(settingKey);

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
      set.add(EventStreams.changesOf(episodeItem.getState().isConsumed()));
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
      boolean missing = episodeItem.getPrimaryStream().isEmpty();
      boolean watched = missing ? false : episodeItem.getState().isConsumed().getValue();

      int seasonNumber = toSeasonBarIndex(episodeItem);

      missingCounts.merge(seasonNumber, missing ? 1 : 0, (a, b) -> a + b);
      watchCounts.merge(seasonNumber, watched ? 1 : 0, (a, b) -> a + b);
      totalCounts.merge(seasonNumber, 1, (a, b) -> a + b);
    }

    for(Integer seasonNumber : totalCounts.keySet()) {
      seasonWatchStates.computeIfAbsent(seasonNumber, k -> Var.newSimpleVar(null))
        .setValue(new SeasonWatchState(totalCounts.get(seasonNumber), missingCounts.get(seasonNumber), watchCounts.get(seasonNumber)));
    }
  }

  private static int toSeasonBarIndex(Work episode) {
    Sequence sequence = episode.getDetails().getSequence().orElseThrow();

    return sequence.getType() == Type.SPECIAL ? 0 : sequence.getType() == Type.EXTRA ? -1 : sequence.getSeasonNumber().orElse(-1);
  }
}
