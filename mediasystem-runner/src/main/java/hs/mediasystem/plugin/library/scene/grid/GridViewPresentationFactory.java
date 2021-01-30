package hs.mediasystem.plugin.library.scene.grid;

import hs.jfx.eventstream.Changes;
import hs.mediasystem.plugin.library.scene.BinderProvider;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.grouping.Grouping;
import hs.mediasystem.runner.grouping.NoGrouping;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.domain.SettingsSource;
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;

import javax.inject.Inject;

import org.reactfx.EventStreams;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

public abstract class GridViewPresentationFactory {
  private static final String SYSTEM_PREFIX = "MediaSystem:Library:Presentation:";

  @Inject private SettingsClient settingsClient;
  @Inject private BinderProvider binderProvider;

  public interface Parent<T> {
    List<T> getChildren();
  }

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
    public final Var<Object> contextItem = Var.newSimpleVar(null);

    private final SettingsSource settingsSource;

    public final Var<SortOrder<U>> sortOrder = Var.newSimpleVar(null);
    public final ObservableList<SortOrder<U>> availableSortOrders = FXCollections.observableArrayList();

    public final Var<Filter<U>> filter = Var.newSimpleVar(null);
    public final ObservableList<Filter<U>> availableFilters = FXCollections.observableArrayList();

    public final Var<Filter<U>> stateFilter = Var.newSimpleVar(null);
    public final ObservableList<Filter<U>> availableStateFilters = FXCollections.observableArrayList();

    public final Var<Grouping<T, U>> grouping = Var.newSimpleVar(null);
    public final ObservableList<Grouping<T, U>> availableGroupings = FXCollections.observableArrayList();

    private final ObservableList<Grouping<T, U>> originalGroupings = FXCollections.observableArrayList();
    private final Var<Object> internalSelectedItem = Var.newSimpleVar(null);
    private final String lastSelectedId;  // Contains the last selected id for this view, or null if there was none stored

    public final Val<Object> selectedItem = internalSelectedItem;
    public final ObservableList<Object> items = FXCollections.observableArrayList();
    public final ObservableList<Group> groups = FXCollections.observableArrayList();

    private final Var<Integer> totalItemCountInternal = Var.newSimpleVar(0);
    private final Var<Integer> visibleUniqueItemCountInternal = Var.newSimpleVar(0);

    public final Val<Integer> totalItemCount = totalItemCountInternal;
    public final Val<Integer> visibleUniqueItemCount = visibleUniqueItemCountInternal;

    private List<U> rootItems;     // Root items (with optional children) with the active grouping applied (unsorted, unfiltered)
    private List<U> rawBaseItems;  // Currently active items, either the root items or a set of children (unsorted, unfiltered)
    private List<U> baseItems;     // Currently active items, either the root items or a set of children (sorted, filtered)


    /**
     * Constructs a new instance.
     *
     * @param settingName a name under which the view settings and last selected item can be stored, cannot be null
     * @param viewOptions a {@link ViewOptions}, cannot be null
     */
    protected GridViewPresentation(String settingName, ViewOptions<T, U> viewOptions) {
      this.settingsSource = settingsClient.of(SYSTEM_PREFIX + settingName);

      this.contextItem.bind(rootContextItem);  // initially bound, can be unbound when navigating to a child
      this.lastSelectedId = settingsSource.getSetting("last-selected");

      this.availableSortOrders.setAll(viewOptions.sortOrders);
      this.availableFilters.setAll(viewOptions.filters);
      this.availableStateFilters.setAll(viewOptions.stateFilters);
      this.availableGroupings.setAll(viewOptions.groupings);
      this.originalGroupings.setAll(viewOptions.groupings);

      this.sortOrder.setValue(viewOptions.sortOrders.get(0));
      this.filter.setValue(viewOptions.filters.get(0));
      this.stateFilter.setValue(viewOptions.stateFilters.get(0));
      this.grouping.setValue(viewOptions.groupings.get(0));

      setupPersistence(settingsSource, binderProvider);
      setupSortingAndFiltering();  // Sets up grouping
    }

    @Override
    public void navigateBack(Event e) {
      if(!this.contextItem.isBound()) {
        this.contextItem.bind(rootContextItem);
        e.consume();
      }
    }

    private void setupPersistence(SettingsSource ss, BinderProvider binderProvider) {
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
          String id = binderProvider.map(IDBinder.class, IDBinder<Object>::toId, current);

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
      this.rootItems = newItems;

      updateRawBaseItems();
    }

    private void updateRawBaseItems() {
      if(contextItem.isBound()) {
        availableGroupings.setAll(originalGroupings);
        setRawBaseItems(rootItems);
      }
      else {
        @SuppressWarnings("unchecked")
        List<U> children = (List<U>)((Parent<T>)contextItem.getValue()).getChildren();

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
      Object itemToSelect = findBestItemToSelect();

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

    private Object findById(String id) {
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

    public void selectItem(Object item) {
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
      Changes.of(inputItems).subscribe(list -> setRootItems(grouping.getValue().group(list)));

      // Changes in sortOrder, filter or stateFilter should update final items:
      EventStreams.merge(
          EventStreams.invalidationsOf(sortOrder),
          EventStreams.invalidationsOf(filter),
          EventStreams.invalidationsOf(stateFilter)
        )
        .observe(e -> updateFinalItemsAndGrouping());

      // Grouping changes require updating root items:
      Changes.of(grouping).subscribe(g -> setRootItems(g.group(inputItems.get())));

      // Navigation to/from parent/child level:
      contextItem.observeInvalidations(oldContextItem -> {
        updateRawBaseItems();  // this already handles selection

        // we override selection here when navigating back from child to parent;
        // the old context item is actually the parent that was shown as context with the child,
        // so this is the one we want to try and select:
        if(contextItem.isBound() && oldContextItem != null) {
          Object obj = findById(toId(oldContextItem));

          if(obj != null) {
            selectItem(obj);
          }
        }
      });
    }

    private String toId(Object object) {
      return binderProvider.map(IDBinder.class, IDBinder<Object>::toId, object);
    }

    /**
     * Finds the best item to select when the unfiltered content is the same but a new filter
     * is applied on it.
     *
     * @return the best item to select, or <code>null</code> if no current item is part of the new filter
     */
    private Object findBestItemToSelect() {
      Predicate<U> predicate = createFilterPredicate();
      Object item = selectedItem.getValue();

      if(item == null && lastSelectedId != null) {
        item = rawBaseItems.stream()
          .filter(i -> toId(i).equals(lastSelectedId))
          .findFirst()
          .orElse(null);
      }

      if(item == null) {  // if previous item isn't available, then donot search for a nearest match
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
