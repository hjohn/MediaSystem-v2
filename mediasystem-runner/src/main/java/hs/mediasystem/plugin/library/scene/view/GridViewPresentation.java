package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.MediaItem.MediaStatus;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.util.javafx.Binds;
import hs.mediasystem.util.javafx.control.gridlistviewskin.Group;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.Event;

import org.reactfx.EventStreams;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

public class GridViewPresentation<T extends MediaDescriptor> extends AbstractPresentation {
  protected static final String SYSTEM_PREFIX = "MediaSystem:Library:Presentation:";

  public final Var<MediaItem<?>> contextItem = Var.newSimpleVar(null);
  
  public final Var<SortOrder<T>> sortOrder = Var.newSimpleVar(null);
  public final ObservableList<SortOrder<T>> availableSortOrders = FXCollections.observableArrayList();

  public final Var<Filter<T>> filter = Var.newSimpleVar(null);
  public final ObservableList<Filter<T>> availableFilters = FXCollections.observableArrayList();

  public final Var<StateFilter> stateFilter = Var.newSimpleVar(StateFilter.ALL);
  public final ObservableList<StateFilter> availableStateFilters = FXCollections.observableArrayList();

  public final Var<Filter<T>> group = Var.newSimpleVar(null);
  public final ObservableList<Filter<T>> availableGroups = FXCollections.observableArrayList();

  private final Var<MediaItem<T>> internalSelectedItem = Var.newSimpleVar(null);

  public final Val<MediaItem<T>> selectedItem = internalSelectedItem;
  public final ObservableList<MediaItem<T>> items = FXCollections.observableArrayList();
  public final ObservableList<Group> groups = FXCollections.observableArrayList();
  public final Val<Integer> totalItemCount;
  public final Val<Integer> visibleUniqueItemCount;

  private final ObservableList<MediaItem<T>> inputItems;
  private final SortedList<MediaItem<T>> sortedItems;
  private final FilteredList<MediaItem<T>> filteredItems;
  private final MediaService mediaService;

  public enum StateFilter {
    ALL, AVAILABLE, UNWATCHED
  }

  protected GridViewPresentation(SettingsSource settingsSource, MediaService mediaService, ObservableList<MediaItem<T>> items, ViewOptions<T> viewOptions, MediaItem<?> contextItem) {
    this.mediaService = mediaService;
    this.inputItems = items;
    this.contextItem.setValue(contextItem);
    this.sortedItems = new SortedList<>(items);  // Sorting first, so we can determine close neighbours when filtering changes
    this.filteredItems = new FilteredList<>(sortedItems);
    this.totalItemCount = Val.create(() -> (int)items.stream().filter(group.getValue().predicate).count(), items, group);
    this.visibleUniqueItemCount = Val.create(() -> (int)items.stream().filter(createFilterPredicate()).count(), items, group, filter, stateFilter);
    
    this.availableSortOrders.setAll(viewOptions.sortOrders);
    this.availableFilters.setAll(viewOptions.filters);
    this.availableStateFilters.setAll(viewOptions.stateFilters);
    this.availableGroups.setAll(viewOptions.groups);

    this.sortOrder.setValue(viewOptions.sortOrders.get(settingsSource.getIntSettingOrDefault("sort-order", 0, 0, viewOptions.sortOrders.size() - 1)));
    this.filter.setValue(viewOptions.filters.get(settingsSource.getIntSettingOrDefault("filter", 0, 0, viewOptions.filters.size() - 1)));
    this.stateFilter.setValue(viewOptions.stateFilters.get(settingsSource.getIntSettingOrDefault("state-filter", 0, 0, viewOptions.stateFilters.size() - 1)));
    this.group.setValue(viewOptions.groups.get(settingsSource.getIntSettingOrDefault("group", 0, 0, viewOptions.groups.size() - 1)));

    this.sortOrder.addListener(obs -> settingsSource.storeIntSetting("sort-order", availableSortOrders.indexOf(sortOrder.getValue())));
    this.filter.addListener(obs -> settingsSource.storeIntSetting("filter", availableFilters.indexOf(filter.getValue())));
    this.stateFilter.addListener(obs -> settingsSource.storeIntSetting("state-filter", availableStateFilters.indexOf(stateFilter.getValue())));
    this.group.addListener(obs -> settingsSource.storeIntSetting("group", availableGroups.indexOf(group.getValue())));

    setupLastSelectedTracking(settingsSource);
    setupSortingAndFiltering();
  }

  private void updateFinalItemsAndGrouping() {
    items.clear();
    groups.clear();

    SortOrder<T> order = sortOrder.getValue();

    if(order.grouper == null) {
      items.addAll(filteredItems);
    }
    else {
      Map<Comparable<Object>, List<MediaItem<T>>> groupedElements = group(filteredItems, order.grouper);
      Comparator<Entry<Comparable<Object>, List<MediaItem<T>>>> comparator = Comparator.comparing(Map.Entry::getKey);

      if(order.reverseGroupOrder) {
        comparator = comparator.reversed();
      }

      List<Entry<Comparable<Object>, List<MediaItem<T>>>> list = groupedElements.entrySet().stream()
        .sorted(comparator)
        .collect(Collectors.toList());

      List<Group> newGroups = new ArrayList<>();
      int position = 0;

      for(Entry<Comparable<Object>, List<MediaItem<T>>> e : list) {
        newGroups.add(new Group(e.getKey().toString().toUpperCase(), position));

        items.addAll(e.getValue());
        position += e.getValue().size();
      }

      groups.addAll(newGroups);
    }
  }

  private static <E, G extends Comparable<G>> Map<Comparable<Object>, List<E>> group(List<E> elements, Function<E, List<Comparable<Object>>> grouper) {
    Map<Comparable<Object>, List<E>> groupedElements = new HashMap<>();

    for(E e : elements) {
      for(Comparable<Object> group : grouper.apply(e)) {
        groupedElements.computeIfAbsent(group, k -> new ArrayList<>()).add(e);
      }
    }

    return groupedElements;
  }

  public void selectItem(MediaItem<T> item) {
    internalSelectedItem.setValue(item);
  }

  public BooleanProperty watchedProperty() {
    MediaItem<?> mediaItem = selectedItem.getValue();

    if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
      return mediaItem.watched;
    }

    return null;  // Indicates no state possible as there is no stream
  }

  public Task<Void> reidentify(Event event) {
    MediaItem<?> mediaItem = selectedItem.getValue();

    event.consume();

    if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
      return new Task<>() {
        @Override
        protected Void call() throws Exception {
          for(BasicStream basicStream : mediaItem.getStreams()) {
            mediaService.reidentify(basicStream.getId());
          }

          return null;
        }
      };

      // TODO after reidentify reload
      // 1) Replace item in list (or reload entire thing)
      // 2) Position may jump, depending on sorting
      // 3) Remember, task method may be called async...
    }

    return null;
  }

  private void setupLastSelectedTracking(SettingsSource settingsSource) {
    String selectedId = settingsSource.getSetting("last-selected");

    if(selectedId != null) {
      for(int i = 0; i < inputItems.size(); i++) {
        MediaItem<T> mediaItem = inputItems.get(i);

        if(mediaItem.getId().equals(selectedId)) {
          internalSelectedItem.setValue(mediaItem);
          break;
        }
      }
    }

    selectedItem.addListener((obs, old, current) -> {
      if(current != null) {
        settingsSource.storeSetting("last-selected", current.getId());
      }
    });
  }

  private Predicate<MediaItem<T>> createFilterPredicate() {
    Predicate<MediaItem<T>> predicate = filter.getValue().predicate;
    Predicate<MediaItem<T>> groupingPredicate = group.getValue().predicate;

    if(stateFilter.getValue() == StateFilter.UNWATCHED) {  // Exclude viewed and not in collection?
      predicate = predicate.and(mi -> mi.mediaStatus.get() == MediaStatus.AVAILABLE);
    }
    if(stateFilter.getValue() == StateFilter.AVAILABLE) {  // Exclude not in collection?
      predicate = predicate.and(mi -> mi.mediaStatus.get() != MediaStatus.UNAVAILABLE);
    }

    return predicate.and(groupingPredicate);
  }

  private void setupSortingAndFiltering() {
    sortedItems.comparatorProperty().bind(Binds.monadic(sortOrder).map(so -> so.comparator));

    EventStreams.merge(
        EventStreams.invalidationsOf(sortOrder),
        EventStreams.invalidationsOf(filter),
        EventStreams.invalidationsOf(stateFilter),
        EventStreams.invalidationsOf(group)
      )
      .withDefaultEvent(null)
      .observe(e -> {
        Predicate<MediaItem<T>> predicate = createFilterPredicate();

        // Before setting the new filter, first determine which item to select based on what
        // is currently available:
        MediaItem<T> itemToSelect = findBestItemToSelect(predicate);

        filteredItems.setPredicate(predicate);
        updateFinalItemsAndGrouping();

        if(itemToSelect == null && !sortedItems.isEmpty()) {
          itemToSelect = sortedItems.get(0);
        }

        selectItem(itemToSelect);
      });
  }

  private MediaItem<T> findBestItemToSelect(Predicate<MediaItem<T>> predicate) {
    MediaItem<T> item = selectedItem.getValue();

    // Find an item as close as possible to the current selected item (including
    // itself) based on the current sort order:

    int previousIndex = sortedItems.indexOf(item);
    int nextIndex = previousIndex + 1;  // Causes it to prefer selecting an item with a higher index when distance to a lower or higher matching item is equal

    while(previousIndex >= 0 || nextIndex < sortedItems.size()) {
      if(previousIndex >= 0) {
        MediaItem<T> candidate = sortedItems.get(previousIndex);

        if(predicate.test(candidate)) {
          return candidate;
        }
      }

      if(nextIndex < sortedItems.size()) {
        MediaItem<T> candidate = sortedItems.get(nextIndex);

        if(predicate.test(candidate)) {
          return candidate;
        }
      }

      previousIndex--;
      nextIndex++;
    }

    // Uh-oh, no item with current filter would also match the new filter; use
    // null to signal to caller it should select item 0.
    return null;
  }

  public static class SortOrder<T extends MediaDescriptor> {
    public final String resourceKey;
    public final Comparator<MediaItem<T>> comparator;
    public final Function<MediaItem<T>, List<Comparable<Object>>> grouper;
    public final boolean reverseGroupOrder;

    @SuppressWarnings("unchecked")
    public <G extends Comparable<G>> SortOrder(String resourceKey, Comparator<MediaItem<T>> comparator, Function<MediaItem<T>, List<G>> grouper, boolean reverseGroupOrder) {
      this.resourceKey = resourceKey;
      this.comparator = comparator;
      this.grouper = (Function<MediaItem<T>, List<Comparable<Object>>>)(Function<?, ?>)grouper;
      this.reverseGroupOrder = reverseGroupOrder;
    }

    public SortOrder(String resourceKey, Comparator<MediaItem<T>> comparator) {
      this(resourceKey, comparator, null, false);
    }
  }

  public static class Filter<T extends MediaDescriptor> {
    public final String resourceKey;
    public final Predicate<MediaItem<T>> predicate;

    public Filter(String resourceKey, Predicate<MediaItem<T>> predicate) {
      this.resourceKey = resourceKey;
      this.predicate = predicate;
    }
  }

  public static class ViewOptions<T extends MediaDescriptor> {
    final List<SortOrder<T>> sortOrders;
    final List<Filter<T>> filters;
    final List<StateFilter> stateFilters;
    final List<Filter<T>> groups;

    public ViewOptions(List<SortOrder<T>> sortOrders, List<Filter<T>> filters, List<StateFilter> stateFilters, List<Filter<T>> groups) {
      this.sortOrders = sortOrders;
      this.filters = filters;
      this.stateFilters = stateFilters;
      this.groups = groups;
    }

    public ViewOptions(List<SortOrder<T>> sortOrders, List<Filter<T>> filters, List<StateFilter> stateFilters) {
      this(sortOrders, filters, stateFilters, List.of(new Filter<T>("none", mi -> true)));
    }
  }
}
