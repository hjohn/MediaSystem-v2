package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.MediaItem.MediaStatus;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.runner.grouping.Grouping;
import hs.mediasystem.runner.grouping.NoGrouping;
import hs.mediasystem.scanner.api.BasicStream;
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
import javafx.concurrent.Task;
import javafx.event.Event;

import org.reactfx.EventStreams;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Base presentation for lists of MediaDescriptors, supporting sorting, grouping and filtering.<p>
 *
 * Note: filtering and grouping use the type T as input, while sorting must support any
 * kind of {@link MediaDescriptor} as grouping may introduce descriptors that are not necessarily
 * of type T.
 *
 * @param <T> a {@link MediaDescriptor} type represented by this presentation
 */
public class GridViewPresentation<T extends MediaDescriptor> extends AbstractPresentation implements Navigable {
  protected static final String SYSTEM_PREFIX = "MediaSystem:Library:Presentation:";

  public final Var<MediaItem<MediaDescriptor>> contextItem = Var.newSimpleVar(null);

  public final Var<SortOrder<MediaDescriptor>> sortOrder = Var.newSimpleVar(null);
  public final ObservableList<SortOrder<MediaDescriptor>> availableSortOrders = FXCollections.observableArrayList();

  public final Var<Filter<T>> filter = Var.newSimpleVar(null);
  public final ObservableList<Filter<T>> availableFilters = FXCollections.observableArrayList();

  public final Var<StateFilter> stateFilter = Var.newSimpleVar(StateFilter.ALL);
  public final ObservableList<StateFilter> availableStateFilters = FXCollections.observableArrayList();

  public final Var<Grouping<T>> grouping = Var.newSimpleVar(null);
  public final ObservableList<Grouping<T>> availableGroupings = FXCollections.observableArrayList();

  private final ObservableList<Grouping<T>> originalGroupings = FXCollections.observableArrayList();
  private final Var<MediaItem<MediaDescriptor>> internalSelectedItem = Var.newSimpleVar(null);

  public final Val<MediaItem<MediaDescriptor>> selectedItem = internalSelectedItem;
  public final ObservableList<MediaItem<MediaDescriptor>> items = FXCollections.observableArrayList();
  public final ObservableList<Group> groups = FXCollections.observableArrayList();

  private final Var<Integer> totalItemCountInternal = Var.newSimpleVar(0);
  private final Var<Integer> visibleUniqueItemCountInternal = Var.newSimpleVar(0);

  public final Val<Integer> totalItemCount = totalItemCountInternal;
  public final Val<Integer> visibleUniqueItemCount = visibleUniqueItemCountInternal;

  private final MediaService mediaService;
  private final List<MediaItem<T>> inputItems;         // Items this presentation was constructed with (ungrouped, unsorted, unfiltered)

  private List<MediaItem<MediaDescriptor>> rootItems;     // Root items (with optional children) with the active grouping applied (unsorted, unfiltered)
  private List<MediaItem<MediaDescriptor>> rawBaseItems;  // Currently active items, either the root items or a set of children (unsorted, unfiltered)
  private List<MediaItem<MediaDescriptor>> baseItems;     // Currently active items, either the root items or a set of children (sorted, filtered)

  public enum StateFilter {
    ALL, AVAILABLE, UNWATCHED
  }

  protected GridViewPresentation(SettingsSource settingsSource, MediaService mediaService, List<MediaItem<T>> inputItems, ViewOptions<T> viewOptions, MediaItem<MediaDescriptor> contextItem) {
    this.mediaService = mediaService;
    this.inputItems = inputItems;
    this.contextItem.setValue(contextItem);

    this.availableSortOrders.setAll(viewOptions.sortOrders);
    this.availableFilters.setAll(viewOptions.filters);
    this.availableStateFilters.setAll(viewOptions.stateFilters);
    this.availableGroupings.setAll(viewOptions.groupings);
    this.originalGroupings.setAll(viewOptions.groupings);

    this.sortOrder.setValue(viewOptions.sortOrders.get(settingsSource.getIntSettingOrDefault("sort-order", 0, 0, viewOptions.sortOrders.size() - 1)));
    this.filter.setValue(viewOptions.filters.get(settingsSource.getIntSettingOrDefault("filter", 0, 0, viewOptions.filters.size() - 1)));
    this.stateFilter.setValue(viewOptions.stateFilters.get(settingsSource.getIntSettingOrDefault("state-filter", 0, 0, viewOptions.stateFilters.size() - 1)));
    this.grouping.setValue(viewOptions.groupings.get(settingsSource.getIntSettingOrDefault("grouping", 0, 0, viewOptions.groupings.size() - 1)));

    this.sortOrder.addListener(obs -> settingsSource.storeIntSetting("sort-order", availableSortOrders.indexOf(sortOrder.getValue())));
    this.filter.addListener(obs -> settingsSource.storeIntSetting("filter", availableFilters.indexOf(filter.getValue())));
    this.stateFilter.addListener(obs -> settingsSource.storeIntSetting("state-filter", availableStateFilters.indexOf(stateFilter.getValue())));
    this.grouping.addListener(obs -> {
      if(availableGroupings.size() > 1) {
        settingsSource.storeIntSetting("grouping", availableGroupings.indexOf(grouping.getValue()));
      }
    });

    setupSortingAndFiltering();                 // Sets up grouping
    setupLastSelectedTracking(settingsSource);  // Sets up correct value for internalSelectedItem based on last selected
  }

  @Override
  public void navigateBack(Event e) {
    MediaItem<MediaDescriptor> contextItem = this.contextItem.getValue();

    if(contextItem != null && !contextItem.getChildren().isEmpty()) {
      this.contextItem.setValue(null);
      e.consume();
    }
  }

  /**
   * Updates the root items, and recursively the base items and final items; required when
   * grouping changes as new parent items can get created.
   *
   * @param newItems the new root items
   */
  private void setRootItems(List<MediaItem<MediaDescriptor>> newItems) {
    this.rootItems = newItems;

    setRawBaseItems(rootItems);
  }

  /**
   * Updates the raw base items and recursively the final items; required when navigating to a
   * parent or child in a hierarchical set of items.
   *
   * @param newRawBaseItems the new base items
   */
  private void setRawBaseItems(List<MediaItem<MediaDescriptor>> newRawBaseItems) {
    this.rawBaseItems = newRawBaseItems;

    updateFinalItemsAndGrouping();
  }

  /**
   * Updates the final items (and applies list based grouping, not to be confused with the
   * hierarchical grouping).
   */
  private void updateFinalItemsAndGrouping() {
    SortOrder<MediaDescriptor> order = sortOrder.getValue();

    this.baseItems = rawBaseItems.stream()
      .filter(createFilterPredicate())
      .sorted(sortOrder.getValue().comparator)
      .collect(Collectors.toList());

    groups.clear();

    if(order.grouper == null || (!(grouping.getValue() instanceof NoGrouping) && !availableGroupings.isEmpty())) {
      items.setAll(baseItems);
    }
    else {
      items.clear();

      Map<Comparable<Object>, List<MediaItem<MediaDescriptor>>> groupedElements = group(baseItems, order.grouper);
      Comparator<Entry<Comparable<Object>, List<MediaItem<MediaDescriptor>>>> comparator = Comparator.comparing(Map.Entry::getKey);

      if(order.reverseGroupOrder) {
        comparator = comparator.reversed();
      }

      List<Entry<Comparable<Object>, List<MediaItem<MediaDescriptor>>>> list = groupedElements.entrySet().stream()
        .sorted(comparator)
        .collect(Collectors.toList());

      List<Group> newGroups = new ArrayList<>();
      int position = 0;

      for(Entry<Comparable<Object>, List<MediaItem<MediaDescriptor>>> e : list) {
        newGroups.add(new Group(e.getKey().toString().toUpperCase(), position));

        items.addAll(e.getValue());
        position += e.getValue().size();
      }

      groups.addAll(newGroups);
    }

    totalItemCountInternal.setValue(rawBaseItems.size());
    visibleUniqueItemCountInternal.setValue(baseItems.size());
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

  public void selectItem(MediaItem<MediaDescriptor> item) {
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
      for(int i = 0; i < rawBaseItems.size(); i++) {
        MediaItem<MediaDescriptor> mediaItem = rawBaseItems.get(i);

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

  private Predicate<MediaItem<MediaDescriptor>> createFilterPredicate() {
    Predicate<MediaItem<T>> predicate = filter.getValue().predicate;

    if(stateFilter.getValue() == StateFilter.UNWATCHED) {  // Exclude viewed and not in collection?
      predicate = predicate.and(mi -> mi.mediaStatus.get() == MediaStatus.AVAILABLE);
    }
    if(stateFilter.getValue() == StateFilter.AVAILABLE) {  // Exclude not in collection?
      predicate = predicate.and(mi -> mi.mediaStatus.get() != MediaStatus.UNAVAILABLE);
    }

    return new HierarchicalPredicate(predicate);
  }

  private void setupSortingAndFiltering() {
    setRootItems(grouping.getValue().group(inputItems));

    EventStreams.merge(
        EventStreams.invalidationsOf(sortOrder),
        EventStreams.invalidationsOf(filter),
        EventStreams.invalidationsOf(stateFilter)
      )
      .withDefaultEvent(null)
      .observe(e -> {
        // Before setting the new filter, first determine which item to select based on what
        // is currently available:
        MediaItem<MediaDescriptor> itemToSelect = findBestItemToSelect();

        updateFinalItemsAndGrouping();

        if(itemToSelect == null && !baseItems.isEmpty()) {
          itemToSelect = baseItems.get(0);
        }

        selectItem(itemToSelect);
      });

    // Grouping changes require updating root items:
    grouping.values().observe(g -> {
      setRootItems(g.group(inputItems));

      selectItem(baseItems.isEmpty() ? null : baseItems.get(0));
    });

    // Navigation to/from parent/child level:
    contextItem.observeInvalidations(oldContextItem -> {
      if(contextItem.getValue() == null || contextItem.getValue().getChildren().isEmpty()) {
        availableGroupings.setAll(originalGroupings);
        setRawBaseItems(rootItems);

        selectItem(oldContextItem);
      }
      else {
        @SuppressWarnings("unchecked")
        List<MediaItem<MediaDescriptor>> children = (List<MediaItem<MediaDescriptor>>)(List<?>)contextItem.getValue().getChildren();

        availableGroupings.clear();
        setRawBaseItems(children);

        selectItem(baseItems.isEmpty() ? null : baseItems.get(0));
      }
    });
  }

  /**
   * Finds the best item to select when the unfiltered content is the same but a new filter
   * is applied on it.
   *
   * @return the best item to select, or <code>null</code> if no current item is part of the new filter
   */
  private MediaItem<MediaDescriptor> findBestItemToSelect() {
    Predicate<MediaItem<MediaDescriptor>> predicate = createFilterPredicate();
    MediaItem<MediaDescriptor> item = selectedItem.getValue();

    // Find an item as close as possible to the current selected item (including
    // itself) based on the current sort order:

    int previousIndex = baseItems.indexOf(item);
    int nextIndex = previousIndex + 1;  // Causes it to prefer selecting an item with a higher index when distance to a lower or higher matching item is equal

    while(previousIndex >= 0 || nextIndex < baseItems.size()) {
      if(previousIndex >= 0) {
        MediaItem<MediaDescriptor> candidate = baseItems.get(previousIndex);

        if(predicate.test(candidate)) {
          return candidate;
        }
      }

      if(nextIndex < baseItems.size()) {
        MediaItem<MediaDescriptor> candidate = baseItems.get(nextIndex);

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
    final List<SortOrder<MediaDescriptor>> sortOrders;
    final List<Filter<T>> filters;
    final List<StateFilter> stateFilters;
    final List<Grouping<T>> groupings;

    public ViewOptions(List<SortOrder<MediaDescriptor>> sortOrders, List<Filter<T>> filters, List<StateFilter> stateFilters, List<Grouping<T>> groupers) {
      this.sortOrders = sortOrders;
      this.filters = filters;
      this.stateFilters = stateFilters;
      this.groupings = groupers;
    }

    public ViewOptions(List<SortOrder<MediaDescriptor>> sortOrders, List<Filter<T>> filters, List<StateFilter> stateFilters) {
      this(sortOrders, filters, stateFilters, List.of(new NoGrouping<T>()));
    }
  }

  class HierarchicalPredicate implements Predicate<MediaItem<MediaDescriptor>> {
    private final Predicate<MediaItem<T>> predicate;

    HierarchicalPredicate(Predicate<MediaItem<T>> predicate) {
      this.predicate = predicate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean test(MediaItem<MediaDescriptor> mi) {
      List<MediaItem<T>> children = (List<MediaItem<T>>)(List<?>)mi.getChildren();

      if(children.isEmpty()) {
        return predicate.test((MediaItem<T>)mi);
      }

      for(MediaItem<T> mediaItem : children) {
        if(predicate.test(mediaItem)) {
          return true;
        }
      }

      return false;
    }
  }
}
