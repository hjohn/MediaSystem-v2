package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.plugin.library.scene.MediaGridView;
import hs.mediasystem.plugin.library.scene.MediaStatus;
import hs.mediasystem.plugin.library.scene.overview.SeasonBar.Entry;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.javafx.ItemSelectedEvent;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.gridlistviewskin.GridListViewSkin.GroupDisplayMode;
import hs.mediasystem.util.javafx.control.gridlistviewskin.Group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class EpisodeListPane extends VBox {
  public final Model model = new Model();

  public static class Model {
    private final ReadOnlyObjectWrapper<List<Work>> internalEpisodes = new ReadOnlyObjectWrapper<>(List.of());
    private final ReadOnlyObjectWrapper<Work> internalSelected = new ReadOnlyObjectWrapper<>();

    /**
     * Displayed list of episodes.
     */
    public final ReadOnlyObjectProperty<List<Work>> episodes = internalEpisodes.getReadOnlyProperty();

    /**
     * Currently selected episode, or <code>null</code> if none is selected.
     */
    public final ReadOnlyObjectProperty<Work> selected = internalSelected.getReadOnlyProperty();

    /**
     * A handler for when a selected item is chosen.
     */
    public final ObjectProperty<EventHandler<ItemSelectedEvent<Work>>> onItemSelected = new SimpleObjectProperty<>();

    public void setEpisodes(List<Work> episodes) {
      if(episodes == null) {
        throw new IllegalArgumentException("episodes cannot be null");
      }

      internalSelected.set(null);  // ensure that the complete state is valid (otherwise something might be temporarily selected that is not part of the list)
      internalEpisodes.set(episodes);
    }

    public void setSelected(Work selected) {
      if(selected != null && !episodes.get().contains(selected)) {
        throw new IllegalArgumentException("selected must be null or be part of episodes: " + selected + " not in " + episodes);
      }

      internalSelected.set(selected);
    }
  }

  private final Map<Integer, Integer> seasonNumberToIndex = new HashMap<>();
  private final MediaGridView<Work> gridView = new MediaGridView<>();

  {
    gridView.visibleRows.set(1);
    gridView.visibleColumns.set(3);
    gridView.setOrientation(Orientation.HORIZONTAL);
    gridView.pageByGroup.set(true);
    gridView.showHeaders.set(false);
    gridView.scrollBarVisible.set(false);
    gridView.groupDisplayMode.set(GroupDisplayMode.FOCUSED);

    VBox.setVgrow(gridView, Priority.ALWAYS);
  }

  private final SeasonBar seasonsBar = new SeasonBar();

  {
    seasonsBar.getStyleClass().add("season-bar");
    seasonsBar.setMinWidth(1);
    seasonsBar.setPrefWidth(1);
  }

  private final VBox outerBox = Containers.vbox("episode-list-dynamic-panel", seasonsBar, gridView);

  {
    VBox.setVgrow(outerBox, Priority.ALWAYS);
  }

  public EpisodeListPane(Callback<ListView<Work>, ListCell<Work>> cellFactory) {
    gridView.setCellFactory(cellFactory);
    gridView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> updateSelection(current));

    model.episodes.addListener((obs, old, current) -> updateList(current));
    model.selected.addListener(obs -> gridView.getSelectionModel().select(model.selected.get()));  // invalidation listener because #select might not work if list does not contain the item
    model.onItemSelected.addListener((obj, old, current) -> gridView.onItemSelected.set(current));

    getChildren().add(outerBox);
  }

  private void updateSelection(Work selected) {
    if(selected != null) {
      Integer value = seasonNumberToIndex.get(toSeasonBarIndex(selected));

      seasonsBar.activeIndex.setValue(value == null ? 0 : value);
    }
    else {
      seasonsBar.activeIndex.setValue(0);
    }

    model.internalSelected.setValue(selected);
  }

  private void updateList(List<Work> episodes) {
    Set<Integer> knownSeasons = new HashSet<>();
    List<Group> groups = new ArrayList<>();
    List<Entry> entries = new ArrayList<>();
    Map<Integer, SeasonWatchState> seasonWatchStates = calculateSeasonWatchStates(episodes);

    seasonNumberToIndex.clear();

    for(int i = 0; i < episodes.size(); i++) {
      Work episode = episodes.get(i);
      int seasonNumber = toSeasonBarIndex(episode);

      if(!knownSeasons.contains(seasonNumber)) {
        knownSeasons.add(seasonNumber);

        groups.add(new Group(seasonNumber == 0 ? "Specials" : seasonNumber == -1 ? "Extras" : "Season " + seasonNumber, i));

        Entry entry;

        switch(seasonNumber) {
        case 0:
          seasonNumberToIndex.put(0, entries.size());
          entry = new Entry("Specials", null, false);
          break;
        case -1:
          seasonNumberToIndex.put(-1, entries.size());
          entry = new Entry("Extras", null, false);
          break;
        default:
          seasonNumberToIndex.put(seasonNumber, entries.size());
          entry = new Entry("" + seasonNumber, null, true);
        }

        SeasonWatchState sws = seasonWatchStates.get(seasonNumber);

        entry.mediaStatus.setValue(sws.totalEpisodes == sws.missingEpisodes ? MediaStatus.UNAVAILABLE : sws.totalEpisodes == sws.watchedEpisodes ? MediaStatus.WATCHED : MediaStatus.AVAILABLE);

        entries.add(entry);
      }
    }

    seasonsBar.entries.setValue(entries);

    gridView.groups.set(groups);
    gridView.itemsProperty().set(FXCollections.observableList(episodes));

    updateSelection(model.selected.get());
  }

  private static Map<Integer, SeasonWatchState> calculateSeasonWatchStates(List<Work> episodes) {
    Map<Integer, SeasonWatchState> seasonWatchStates = new HashMap<>();
    Map<Integer, Integer> missingCounts = new HashMap<>();
    Map<Integer, Integer> watchCounts = new HashMap<>();
    Map<Integer, Integer> totalCounts = new HashMap<>();

    for(Work episodeItem : episodes) {
      boolean missing = episodeItem.getPrimaryStream().isEmpty();
      boolean watched = missing ? false : episodeItem.getState().consumed();

      int seasonNumber = toSeasonBarIndex(episodeItem);

      missingCounts.merge(seasonNumber, missing ? 1 : 0, (a, b) -> a + b);
      watchCounts.merge(seasonNumber, watched ? 1 : 0, (a, b) -> a + b);
      totalCounts.merge(seasonNumber, 1, (a, b) -> a + b);
    }

    for(Integer seasonNumber : totalCounts.keySet()) {
      seasonWatchStates.put(seasonNumber, new SeasonWatchState(totalCounts.get(seasonNumber), missingCounts.get(seasonNumber), watchCounts.get(seasonNumber)));
    }

    return seasonWatchStates;
  }

  private static int toSeasonBarIndex(Work episode) {
    Sequence sequence = episode.getDetails().getSequence().orElseThrow();

    return sequence.type() == Type.SPECIAL ? 0 : sequence.type() == Type.EXTRA ? -1 : sequence.seasonNumber().orElse(-1);
  }

  private static class SeasonWatchState {
    public final int totalEpisodes;
    public final int missingEpisodes;
    public final int watchedEpisodes;

    public SeasonWatchState(int totalEpisodes, int missingEpisodes, int watchedEpisodes) {
      this.totalEpisodes = totalEpisodes;
      this.missingEpisodes = missingEpisodes;
      this.watchedEpisodes = watchedEpisodes;
    }
  }
}
