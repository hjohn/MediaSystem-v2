package hs.mediasystem.plugin.library.scene.overview;

import hs.jfx.eventstream.Transactions;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.StreamStateClient;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Parent;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.SettingsSource;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.javafx.property.SimpleReadOnlyObjectProperty;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.EventSource;

@Singleton
public class ProductionPresentationFactory {
  private static final String SYSTEM = "MediaSystem:Episode";

  @Inject private WorkClient workClient;
  @Inject private SettingsClient settingsClient;
  @Inject private StreamStateClient streamStateClient;

  public ProductionPresentation create(WorkId id) {
    ProductionPresentation presentation = new ProductionPresentation();

    Work work = queryWork(id);
    WorkId rootId = work.getType().isComponent() ? work.getParent().map(Parent::getId).orElse(id) : id;
    WorkId selectedChildId = rootId.equals(id) ? null : id;
    State state = rootId.equals(id) ? State.OVERVIEW : State.EPISODE;

    presentation.refresh(rootId, state, selectedChildId).run();

    return presentation;
  }

  private Work queryWork(WorkId id) {
    return workClient.find(id).orElseThrow();
  }

  private List<Work> queryChildren(WorkId id) {
    return workClient.findChildren(id).stream()
      .sorted(Comparator.comparing(w -> w.getDetails().getSequence().orElseThrow(), Sequence.COMPARATOR))
      .collect(Collectors.toList());
  }

  public enum State {
    OVERVIEW, LIST, EPISODE
  }

  public class ProductionPresentation extends AbstractPresentation implements Navigable {
    private final SettingsSource settingsSource = settingsClient.of(SYSTEM);

    // Internal properties:
    private final ReadOnlyObjectWrapper<Work> internalRoot = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<List<Work>> internalChildren = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyDoubleWrapper internalWatchedFraction = new ReadOnlyDoubleWrapper();  // Of top level item (Movie or Serie)
    private final ReadOnlyDoubleWrapper internalMissingFraction = new ReadOnlyDoubleWrapper();  // Of top level item (Serie only)
    private final ObjectProperty<State> internalState = new SimpleObjectProperty<>(State.OVERVIEW);

    // Public read only properties:
    public final ReadOnlyObjectProperty<Work> root = internalRoot.getReadOnlyProperty();
    public final ReadOnlyObjectProperty<List<Work>> children = internalChildren.getReadOnlyProperty();  // sorted
    public final ReadOnlyDoubleProperty watchedFraction = internalWatchedFraction.getReadOnlyProperty();  // Of top level item (Movie or Serie)
    public final ReadOnlyDoubleProperty missingFraction = internalMissingFraction.getReadOnlyProperty();  // Of top level item (Serie only)
    public final ReadOnlyObjectProperty<State> state = new SimpleReadOnlyObjectProperty<>(internalState);

    // Public mutable properties:
    public final ObjectProperty<Work> selectedChild = new SimpleObjectProperty<>();  // can be changed directly

    // Events:
    public final EventSource<Event> showInfo = new EventSource<>();

    private ProductionPresentation() {
      selectedChild.addListener((obs, old, current) ->
        current.getParent().ifPresent(p -> settingsSource.storeSetting("last-selected:" + p.getId(), current.getId().toString()))
      );
    }

    @Override
    public Runnable createUpdateTask() {
      return refresh(
        root.get().getId(),
        internalState.get(),
        selectedChild.get() == null ? null : selectedChild.get().getId()
      );
    }

    private Runnable refresh(WorkId rootId, State newState, WorkId newSelectedChild) {
      Work newRoot = queryWork(rootId);
      List<Work> newChildren = queryChildren(rootId);

      return () -> update(newRoot, newState, newChildren, newSelectedChild);
    }

    public void update(Work root, State state, List<Work> children, WorkId selectedChildId) {
      Transactions.doWhile(() -> {
        this.internalRoot.set(root);
        this.internalChildren.set(children);

        if(!children.isEmpty()) {
          String id = selectedChildId == null ? settingsSource.getSetting("last-selected:" + root.getId()) : selectedChildId.toString();

          selectChild(id);
        }

        this.internalState.set(state);

        internalWatchedFraction.set(getWatchedFraction(root, children));
        internalMissingFraction.set(getMissingFraction(root, children));
      });
    }

    private void selectChild(String id) {
      List<Work> episodes = children.get();

      if(id != null) {
        for(Work work : episodes) {
          if(id.equals(work.getId().toString())) {
            selectedChild.setValue(work);
            break;
          }
        }
      }
      else if(!episodes.isEmpty()) {
        selectedChild.setValue(episodes.get(0));
      }
    }

    @Override
    public void navigateBack(Event e) {
      switch(state.get()) {
      case OVERVIEW:
        return;
      case LIST:
        internalState.set(State.OVERVIEW);
        break;
      case EPISODE:
        internalState.set(State.LIST);
        break;
      }

      e.consume();
    }

    public void showInfo(Event e) {
      showInfo.push(e);
    }

    public void toEpisodeState() {
      if(selectedChild.getValue() == null) {
        throw new IllegalStateException("Cannot go to Episode state without an episode set");
      }

      this.internalState.set(State.EPISODE);
    }

    public void toListState() {
      if(children.get().isEmpty()) {
        throw new IllegalStateException("Cannot go to List state if root item is not a Serie");
      }

      this.internalState.set(State.LIST);
    }

    public BooleanProperty watchedProperty() {
      Work rootItem = root.get();

      if(rootItem.getType().isPlayable() && !rootItem.getStreams().isEmpty()) {
        return streamStateClient.watchedProperty(rootItem.getPrimaryStream().orElseThrow().getId().getContentId());
      }

      return null;  // Indicates no state possible as there is no stream or is a serie
    }

    public BooleanProperty episodeWatchedProperty() {
      if(internalState.get() != State.OVERVIEW) {
        Work work = selectedChild.getValue();

        if(work != null && !work.getStreams().isEmpty()) {
          return streamStateClient.watchedProperty(work.getPrimaryStream().orElseThrow().getId().getContentId());
        }
      }

      return null;  // Indicates no state possible as there is no stream
    }

    public Property<Boolean> seasonWatchedProperty() {  // tri-state property
      Work currentItem = selectedChild.getValue();

      if(internalState.get() == State.OVERVIEW || currentItem == null || currentItem.getStreams().isEmpty()) {
        return null;
      }

      List<Work> seasonEpisodes;
      List<Work> initialWatchedEpisodes;

      seasonEpisodes = children.get().stream()
        .filter(w -> w.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) == currentItem.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0))
        .collect(Collectors.toList());

      initialWatchedEpisodes = seasonEpisodes.stream()
        .filter(w -> w.getState().isConsumed())
        .collect(Collectors.toList());

      Property<Boolean> seasonWatchedProperty = new SimpleObjectProperty<>();

      seasonWatchedProperty.setValue(initialWatchedEpisodes.isEmpty() ? Boolean.FALSE :
               initialWatchedEpisodes.size() == seasonEpisodes.size() ? Boolean.TRUE : null);

      seasonWatchedProperty.addListener((obs, old, current) -> {
        for(Work episode : seasonEpisodes) {
          BooleanProperty watched = streamStateClient.watchedProperty(episode.getPrimaryStream().orElseThrow().getId().getContentId());

          watched.setValue(current == null ? initialWatchedEpisodes.contains(episode) : Boolean.TRUE.equals(current));
        }
      });

      return seasonWatchedProperty;
    }
  }

  /**
   * Returns a value indicating whether the item is watched, partially watched, unwatched or missing.
   *
   * A value of 1.0 indicates a watched item.
   * A value between 0.0 (exclusively) and 1.0 (exclusively) indicates a partially watched item.
   * A value of 0.0 indicates an unwatched item.
   * A negative value indicates a missing item.
   *
   * @return a value indicating whether the item is watched, partially watched, unwatched or missing
   */
  private static double getWatchedFraction(Work rootItem, List<Work> episodeItems) {
    boolean rootWatched = rootItem.getState().isConsumed();
    boolean rootMissing = rootItem.getStreams().isEmpty();

    if(rootWatched) {
      return 1.0;
    }

    if(rootMissing) {
      return -1.0;
    }

    if(rootItem.getType().isPlayable()) {
      Integer totalDuration = rootItem.getPrimaryStream()
        .flatMap(ms -> ms.getDuration().map(d -> (int)d.toSeconds()))
        .orElse(null);

      return totalDuration == null ? 0.0 : rootItem.getState().getResumePosition().toSeconds() / (double)totalDuration;
    }

    if(rootItem.getType().isSerie()) {
      long totalWatched = episodeItems.stream()
        .filter(i -> i.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) > 0)
        .filter(i -> i.getState().isConsumed())
        .count();

      long total = episodeItems.stream()
        .filter(i -> i.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) > 0)
        .count();

      return totalWatched / (double)total;
    }

    return 0.0;
  }

  private static double getMissingFraction(Work rootItem, List<Work> episodeItems) {
    boolean rootWatched = rootItem.getState().isConsumed();
    boolean rootMissing = rootItem.getStreams().isEmpty();

    if(rootItem.getType().isSerie() && !rootWatched && !rootMissing) {
      long totalMissingUnwatched = episodeItems.stream()
        .filter(i -> i.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) > 0)
        .filter(i -> i.getStreams().isEmpty() && !i.getState().isConsumed())
        .count();

      long total = episodeItems.stream()
        .filter(i -> i.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) > 0)
        .count();

      return totalMissingUnwatched / (double)total;
    }

    return 0.0;
  }
}
