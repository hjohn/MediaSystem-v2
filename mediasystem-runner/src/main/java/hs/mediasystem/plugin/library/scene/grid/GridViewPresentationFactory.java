package hs.mediasystem.plugin.library.scene.grid;

import hs.jfx.eventstream.core.Changes;
import hs.jfx.eventstream.core.Events;
import hs.jfx.eventstream.core.Invalidations;
import hs.jfx.eventstream.core.Values;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.presentation.Navigable;
import hs.mediasystem.runner.grouping.Grouping;
import hs.mediasystem.runner.grouping.NoGrouping;
import hs.mediasystem.runner.grouping.WorksGroup;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.SettingsSource;
import hs.mediasystem.util.javafx.ui.gridlistviewskin.Group;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;

import javax.inject.Inject;

public abstract class GridViewPresentationFactory {
  private static final String SYSTEM_PREFIX = "MediaSystem:Library:Presentation:";

  @Inject private SettingsClient settingsClient;

  /**
   * A presentation of a list of items, with support for sorting, filtering and
   * grouping.  Keeps track of last selection.
   *
   * @param <T> the type supplied to the presentation
   * @param <U> the type resulting from grouping
   */
  public class GridViewPresentation<T, U> extends AbstractPresentation implements Navigable {

    // Model
    public final ObjectProperty<Object> rootContextItem = new SimpleObjectProperty<>();
    public final ObjectProperty<List<? extends T>> inputItems = new SimpleObjectProperty<>(List.of());   // Items this presentation was constructed with (ungrouped, unsorted, unfiltered)

    /**
     * Contains the item that is relevant for the current active items in this presentation.  This
     * may be a Parent<T>, but can also be an unrelated item or null if this was supplied as root
     * context.
     */
    public final ObjectProperty<Object> contextItem = new SimpleObjectProperty<>(null);

    private final SettingsSource settingsSource;
    private final Function<U, Object> idProvider;

    public final ObjectProperty<SortOrder<U>> sortOrder = new SimpleObjectProperty<>(null);
    public final ObservableList<SortOrder<U>> availableSortOrders = FXCollections.observableArrayList();

    public final ObjectProperty<Filter<U>> filter = new SimpleObjectProperty<>(null);
    public final ObservableList<Filter<U>> availableFilters = FXCollections.observableArrayList();

    public final ObjectProperty<Filter<U>> stateFilter = new SimpleObjectProperty<>(null);
    public final ObservableList<Filter<U>> availableStateFilters = FXCollections.observableArrayList();

    public final ObjectProperty<Grouping<T, U>> grouping = new SimpleObjectProperty<>(null);
    public final ObservableList<Grouping<T, U>> availableGroupings = FXCollections.observableArrayList();

    private final ObservableList<Grouping<T, U>> originalGroupings = FXCollections.observableArrayList();
    private final ObjectProperty<U> internalSelectedItem = new SimpleObjectProperty<>(null);

    public final ReadOnlyObjectProperty<U> selectedItem = internalSelectedItem;  // TODO this could be cast back, not as safe as wrapper
    public final ObservableList<U> items = FXCollections.observableArrayList();
    public final ObservableList<Group> groups = FXCollections.observableArrayList();

    private final IntegerProperty totalItemCountInternal = new SimpleIntegerProperty(0);
    private final IntegerProperty visibleUniqueItemCountInternal = new SimpleIntegerProperty(0);

    public final ReadOnlyIntegerProperty totalItemCount = totalItemCountInternal;   // TODO this could be cast back, not as safe as wrapper
    public final ReadOnlyIntegerProperty visibleUniqueItemCount = visibleUniqueItemCountInternal;   // TODO this could be cast back, not as safe as wrapper

    private List<U> rootItems;     // Root items (with optional children) with the active grouping applied (unsorted, unfiltered)
    private List<U> rawBaseItems;  // Currently active items, either the root items or a set of children (unsorted, unfiltered)
    private List<U> baseItems;     // Currently active items, either the root items or a set of children (sorted, filtered)

    /**
     * Constructs a new instance.
     *
     * @param settingName a name under which the view settings and last selected item can be stored, cannot be {@code null}
     * @param viewOptions a {@link ViewOptions}, cannot be {@code null}
     * @param idProvider a function converting a type {@code U} to an id object, cannot be {@code null}
     */
    protected GridViewPresentation(String settingName, ViewOptions<T, U> viewOptions, Function<U, Object> idProvider) {
      this.settingsSource = settingsClient.of(SYSTEM_PREFIX + Objects.requireNonNull(settingName, "settingName cannot be null"));
      this.idProvider = Objects.requireNonNull(idProvider, "idProvider cannot be null");

      this.contextItem.bind(rootContextItem);  // initially bound, can be unbound when navigating to a child

      this.availableSortOrders.setAll(viewOptions.sortOrders);
      this.availableFilters.setAll(viewOptions.filters);
      this.availableStateFilters.setAll(viewOptions.stateFilters);
      this.availableGroupings.setAll(viewOptions.groupings);
      this.originalGroupings.setAll(viewOptions.groupings);

      this.sortOrder.setValue(viewOptions.sortOrders.get(0));
      this.filter.setValue(viewOptions.filters.get(0));
      this.stateFilter.setValue(viewOptions.stateFilters.get(0));
      this.grouping.setValue(viewOptions.groupings.get(0));

      setupPersistence(settingsSource);
      setupSortingAndFiltering();  // Sets up grouping
    }

    @Override
    public void navigateBack(Event e) {
      if(!this.contextItem.isBound()) {
        this.contextItem.bind(rootContextItem);
        e.consume();
      }
    }

    private void setupPersistence(SettingsSource ss) {
      this.sortOrder.setValue(this.availableSortOrders.get(ss.getIntSettingOrDefault("sort-order", 0, 0, this.availableSortOrders.size() - 1)));
      this.filter.setValue(this.availableFilters.get(ss.getIntSettingOrDefault("filter", 0, 0, this.availableFilters.size() - 1)));
      this.stateFilter.setValue(this.availableStateFilters.get(ss.getIntSettingOrDefault("state-filter", 0, 0, this.availableStateFilters.size() - 1)));

      if(this.availableGroupings.size() > 0) {
        this.grouping.setValue(this.availableGroupings.get(ss.getIntSettingOrDefault("grouping", 0, 0, this.availableGroupings.size() - 1)));
      }

      this.sortOrder.addListener(obs -> ss.storeIntSetting("sort-order", this.availableSortOrders.indexOf(this.sortOrder.getValue())));
      this.filter.addListener(obs -> ss.storeIntSetting("filter", this.availableFilters.indexOf(this.filter.getValue())));
      this.stateFilter.addListener(obs -> ss.storeIntSetting("state-filter", this.availableStateFilters.indexOf(this.stateFilter.getValue())));
      this.grouping.addListener(obs -> {
        if(this.availableGroupings.size() > 1) {
          ss.storeIntSetting("grouping", this.availableGroupings.indexOf(this.grouping.getValue()));
        }
      });

      this.selectedItem.addListener((obs, old, current) -> {
        if(current != null) {
          String id = idProvider.apply(current).toString();

          ss.storeSetting("last-selected", id);
        }
      });
    }

    /**
     * Updates the root items, and recursively the base items and final items; required when
     * grouping changes as new parent items can get created.
     *
     * @param newItems the new root items
     */
    private void setRootItems(List<U> newItems) {
      this.rootItems = Objects.requireNonNull(newItems, "newItems");

      if(!contextItem.isBound()) {

        /*
         * Replace the context item (and its children) with a newer version when all items
         * have been replaced.
         */

        @SuppressWarnings("unchecked")
        U castContextItem = (U)contextItem.get();
        String contextItemId = toId(castContextItem);
        boolean found = false;

        for(U item : rootItems) {
          if(contextItemId.equals(toId(item))) {
            found = true;
            contextItem.set(item);  // replace with new version
            break;
          }
        }

        if(!found) {  // if context item does not exist anymore, switch back to root
          contextItem.bind(rootContextItem);
        }
      }

      updateRawBaseItems();
    }

    private void updateRawBaseItems() {
      if(contextItem.isBound()) {
        availableGroupings.setAll(originalGroupings);
        setRawBaseItems(rootItems);
      }
      else {
        @SuppressWarnings("unchecked")
        List<U> children = (List<U>)((WorksGroup)contextItem.getValue()).getChildren();

        availableGroupings.clear();
        setRawBaseItems(children);
      }
    }

    /**
     * Updates the raw base items and recursively the final items; required when navigating to a
     * parent or child in a hierarchical set of items.
     *
     * @param newRawBaseItems the new base items
     */
    private void setRawBaseItems(List<U> newRawBaseItems) {
      this.rawBaseItems = newRawBaseItems;

      updateFinalItemsAndGrouping();
    }

    /**
     * Updates the final items (and applies list based grouping, not to be confused with the
     * hierarchical grouping).
     */
    private void updateFinalItemsAndGrouping() {

      // Before changing the final items, first determine which item to select based on what
      // is currently available:
      U itemToSelect = findBestItemToSelect();

      SortOrder<U> order = sortOrder.getValue();

      this.baseItems = rawBaseItems.stream()
        .filter(createFilterPredicate())
        .sorted(order.comparator)
        .collect(Collectors.toList());

      groups.clear();

      if(order.grouper == null || (!(grouping.getValue() instanceof NoGrouping) && !availableGroupings.isEmpty())) {
        items.setAll(baseItems);
      }
      else {
        items.clear();

        Map<Comparable<Object>, List<U>> groupedElements = group(baseItems, order.grouper);
        Comparator<Entry<Comparable<Object>, List<U>>> comparator = Comparator.comparing(Map.Entry::getKey);

        if(order.reverseGroupOrder) {
          comparator = comparator.reversed();
        }

        List<Entry<Comparable<Object>, List<U>>> list = groupedElements.entrySet().stream()
          .sorted(comparator)
          .collect(Collectors.toList());

        List<Group> newGroups = new ArrayList<>();
        int position = 0;

        for(Entry<Comparable<Object>, List<U>> e : list) {
          newGroups.add(new Group(e.getKey().toString().toUpperCase(), position));

          items.addAll(e.getValue());
          position += e.getValue().size();
        }

        groups.addAll(newGroups);
      }

      totalItemCountInternal.setValue(rawBaseItems.size());
      visibleUniqueItemCountInternal.setValue(baseItems.size());

      /*
       * Handle selection
       */

      if(itemToSelect != null) {
        String itemToSelectId = toId(itemToSelect);

        // As itemToSelect might be an older instance of the item wanted (due to refresh), find the latest instance of it:
        itemToSelect = findById(itemToSelectId);
      }

      if(itemToSelect == null && !baseItems.isEmpty()) {
        itemToSelect = baseItems.get(0);
      }

      selectItem(itemToSelect);
    }

    private U findById(String id) {
      return items.stream()
        .filter(i -> toId(i).equals(id))
        .findFirst()
        .orElse(null);
    }

    private <G extends Comparable<G>> Map<Comparable<Object>, List<U>> group(List<U> elements, Function<U, List<Comparable<Object>>> grouper) {
      Map<Comparable<Object>, List<U>> groupedElements = new HashMap<>();

      for(U e : elements) {
        for(Comparable<Object> group : grouper.apply(e)) {
          groupedElements.computeIfAbsent(group, k -> new ArrayList<>()).add(e);
        }
      }

      return groupedElements;
    }

    public void selectItem(U item) {
      internalSelectedItem.setValue(item);
    }

    private Predicate<U> createFilterPredicate() {
      Predicate<U> predicate = filter.getValue().predicate;

      if(stateFilter.getValue() != null) {
        predicate = predicate.and(stateFilter.getValue().predicate);
      }

      return predicate;
    }

    private void setupSortingAndFiltering() {
      Values.of(inputItems).subscribe(list -> {

        /*
         * When input items change, there should be no relation to whatever was part of
         * this presentation previously. Reset the derived values so no attempt is made
         * to select the closest item in the new set based on an unrelated previous set.
         */

        baseItems = null;
        rawBaseItems = null;

        setRootItems(grouping.getValue().group(list));
      });

      // Changes in sortOrder, filter or stateFilter should update final items:
      Invalidations.of(sortOrder, filter, stateFilter)
        .subscribe(obs -> updateFinalItemsAndGrouping());

      // Grouping changes require updating root items:
      Events.of(grouping).subscribe(g -> setRootItems(g.group(inputItems.get())));

      // Navigation to/from parent/child level:

      Changes.of(contextItem)
        .subscribe(c -> {
          updateRawBaseItems();  // this already handles selection

          // we override selection here when navigating back from child to parent;
          // the old context item is actually the parent that was shown as context with the child,
          // so this is the one we want to try and select:
          if(contextItem.isBound() && c.getOldValue() != null) {
            @SuppressWarnings("unchecked")
            U oldValue = (U)c.getOldValue();
            U obj = findById(toId(oldValue));

            if(obj != null) {
              selectItem(obj);
            }
          }
        });
    }

    private String toId(U object) {
      return idProvider.apply(object).toString();
    }

    /**
     * Finds the best item to select when the unfiltered content is the same but a new filter
     * is applied on it.
     *
     * @return the best item to select, or <code>null</code> if no current item is part of the new filter
     */
    private U findBestItemToSelect() {
      Predicate<U> predicate = createFilterPredicate();
      U item = selectedItem.getValue();
      String lastSelectedId = settingsSource.getSetting("last-selected");

      if(item == null && lastSelectedId != null) {
        item = rawBaseItems.stream()
          .filter(i -> toId(i).equals(lastSelectedId))
          .findFirst()
          .orElse(null);
      }

      if(item == null) {  // if previous item isn't available, then do not search for a nearest match
        return null;
      }

      if(baseItems == null) {  // if there weren't any previous items, then can't find a best item either
        return item;
      }

      /*
       * Find an item as close as possible to the current selected item (including
       * itself) based on the current sort order:
       */

      int previousIndex = baseItems.indexOf(item);
      int nextIndex = previousIndex + 1;  // Causes it to prefer selecting an item with a higher index when distance to a lower or higher matching item is equal

      while(previousIndex >= 0 || nextIndex < baseItems.size()) {
        if(previousIndex >= 0) {
          U candidate = baseItems.get(previousIndex);

          if(predicate.test(candidate)) {
            return candidate;
          }
        }

        if(nextIndex < baseItems.size()) {
          U candidate = baseItems.get(nextIndex);

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
  }

  public static class SortOrder<T> {
    public final String resourceKey;
    public final Comparator<T> comparator;
    public final Function<T, List<Comparable<Object>>> grouper;
    public final boolean reverseGroupOrder;

    /**
     * Constructs a new instance.
     *
     * @param <G> the type of instance resulting from groupings (only need to be Comparable)
     * @param resourceKey a resource key for localization
     * @param comparator a comparator for sorting
     * @param grouper a function which takes an item and returns a list of groups it belongs (can be more than one)
     * @param reverseGroupOrder whether the order of the groups created should be reversed
     */
    @SuppressWarnings("unchecked")
    public <G extends Comparable<G>> SortOrder(String resourceKey, Comparator<T> comparator, Function<T, List<G>> grouper, boolean reverseGroupOrder) {
      if(comparator == null) {
        throw new IllegalArgumentException("comparator cannot be null");
      }

      this.resourceKey = resourceKey;
      this.comparator = comparator;
      this.grouper = (Function<T, List<Comparable<Object>>>)(Function<?, ?>)grouper;
      this.reverseGroupOrder = reverseGroupOrder;
    }

    public SortOrder(String resourceKey, Comparator<T> comparator) {
      this(resourceKey, comparator, null, false);
    }
  }

  public static class Filter<T> {
    public final String resourceKey;
    public final Predicate<T> predicate;

    public Filter(String resourceKey, Predicate<T> predicate) {
      if(predicate == null) {
        throw new IllegalArgumentException("predicate cannot be null");
      }

      this.resourceKey = resourceKey;
      this.predicate = predicate;
    }
  }

  /**
   * Defines how the presentation can be sorted, filtered and grouped.<p>
   *
   * Sorting and filtering must be able to handle the return types of groupings.<p>
   *
   * Groupings only ever act on the types supplied to the presentation.
   *
   * @param <T> the type supplied to the presentation
   * @param <U> the type resulting from grouping
   */
  public static class ViewOptions<T, U> {
    final List<SortOrder<U>> sortOrders;
    final List<Filter<U>> filters;
    final List<Filter<U>> stateFilters;
    final List<Grouping<T, U>> groupings;

    public ViewOptions(List<SortOrder<U>> sortOrders, List<Filter<U>> filters, List<Filter<U>> stateFilters, List<Grouping<T, U>> groupings) {
      this.sortOrders = sortOrders;
      this.filters = filters;
      this.stateFilters = stateFilters;
      this.groupings = groupings;
    }

    public ViewOptions(List<SortOrder<U>> sortOrders, List<Filter<U>> filters, List<Filter<U>> stateFilters) {
      this(sortOrders, filters, stateFilters, List.of(new NoGrouping<T, U>()));
    }
  }
}
