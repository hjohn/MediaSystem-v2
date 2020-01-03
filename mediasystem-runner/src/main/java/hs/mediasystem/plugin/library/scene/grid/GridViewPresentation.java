package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.grouping.Grouping;
import hs.mediasystem.runner.grouping.NoGrouping;
import hs.mediasystem.util.javafx.control.gridlistviewskin.Group;

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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;

import org.reactfx.EventStreams;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Presentation for presenting a list of objects as a grid, supporting sorting, grouping and filtering.<p>
 *
 * Note that {@link Grouping}s can create other objects not of type T that will be part of the grid.
 * This is why selectedItem and items are {@link Object}s, and not type T.
 *
 * @param <T> the type of input objects for this presentation
 */
public class GridViewPresentation<T> extends AbstractPresentation implements Navigable {

  public interface Parent<T> {
    List<T> getChildren();
  }

  protected static final String SYSTEM_PREFIX = "MediaSystem:Library:Presentation:";

  /**
   * Contains the item that is relevant for the current active items in this presentation.  This
   * may be a Parent<T>, but can also be an unrelated item or null if this was supplied as root
   * context.
   */
  public final Var<Object> contextItem = Var.newSimpleVar(null);

  private final Object rootContextItem;

  public final Var<SortOrder<T>> sortOrder = Var.newSimpleVar(null);
  public final ObservableList<SortOrder<T>> availableSortOrders = FXCollections.observableArrayList();

  public final Var<Filter<T>> filter = Var.newSimpleVar(null);
  public final ObservableList<Filter<T>> availableFilters = FXCollections.observableArrayList();

  public final Var<Filter<T>> stateFilter = Var.newSimpleVar(null);
  public final ObservableList<Filter<T>> availableStateFilters = FXCollections.observableArrayList();

  public final Var<Grouping<T>> grouping = Var.newSimpleVar(null);
  public final ObservableList<Grouping<T>> availableGroupings = FXCollections.observableArrayList();

  private final ObservableList<Grouping<T>> originalGroupings = FXCollections.observableArrayList();
  private final Var<Object> internalSelectedItem = Var.newSimpleVar(null);

  public final Val<Object> selectedItem = internalSelectedItem;
  public final ObservableList<Object> items = FXCollections.observableArrayList();
  public final ObservableList<Group> groups = FXCollections.observableArrayList();

  private final Var<Integer> totalItemCountInternal = Var.newSimpleVar(0);
  private final Var<Integer> visibleUniqueItemCountInternal = Var.newSimpleVar(0);

  public final Val<Integer> totalItemCount = totalItemCountInternal;
  public final Val<Integer> visibleUniqueItemCount = visibleUniqueItemCountInternal;

  private final List<T> inputItems;   // Items this presentation was constructed with (ungrouped, unsorted, unfiltered)

  private List<Object> rootItems;     // Root items (with optional children) with the active grouping applied (unsorted, unfiltered)
  private List<Object> rawBaseItems;  // Currently active items, either the root items or a set of children (unsorted, unfiltered)
  private List<Object> baseItems;     // Currently active items, either the root items or a set of children (sorted, filtered)

  protected GridViewPresentation(List<T> inputItems, ViewOptions<T> viewOptions, Object rootContextItem) {
    this.inputItems = inputItems;
    this.rootContextItem = rootContextItem;
    this.contextItem.setValue(rootContextItem);

    this.availableSortOrders.setAll(viewOptions.sortOrders);
    this.availableFilters.setAll(viewOptions.filters);
    this.availableStateFilters.setAll(viewOptions.stateFilters);
    this.availableGroupings.setAll(viewOptions.groupings);
    this.originalGroupings.setAll(viewOptions.groupings);

    this.sortOrder.setValue(viewOptions.sortOrders.get(0));
    this.filter.setValue(viewOptions.filters.get(0));
    this.stateFilter.setValue(viewOptions.stateFilters.get(0));
    this.grouping.setValue(viewOptions.groupings.get(0));

    setupSortingAndFiltering();  // Sets up grouping
  }

  @Override
  public void navigateBack(Event e) {
    if(!Objects.equals(this.contextItem.getValue(), rootContextItem)) {
      this.contextItem.setValue(rootContextItem);
      e.consume();
    }
  }

  /**
   * Updates the root items, and recursively the base items and final items; required when
   * grouping changes as new parent items can get created.
   *
   * @param newItems the new root items
   */
  private void setRootItems(List<Object> newItems) {
    this.rootItems = newItems;

    setRawBaseItems(rootItems);
  }

  /**
   * Updates the raw base items and recursively the final items; required when navigating to a
   * parent or child in a hierarchical set of items.
   *
   * @param newRawBaseItems the new base items
   */
  private void setRawBaseItems(List<Object> newRawBaseItems) {
    this.rawBaseItems = newRawBaseItems;

    updateFinalItemsAndGrouping();
  }

  /**
   * Updates the final items (and applies list based grouping, not to be confused with the
   * hierarchical grouping).
   */
  private void updateFinalItemsAndGrouping() {
    SortOrder<T> order = sortOrder.getValue();

    /*
     * Note, the SortOrder here is only intended for items of type T, not special
     * items created for hierarchical grouping.  Therefore it is wrapped in a
     * comparator that uses the first child of an item if available as that should
     * be of type T.
     */

    HierachicalComparator hierarchicalComparator = new HierachicalComparator(order.comparator);

    this.baseItems = rawBaseItems.stream()
      .filter(createFilterPredicate())
      .sorted(hierarchicalComparator)
      .collect(Collectors.toList());

    groups.clear();

    if(order.grouper == null || (!(grouping.getValue() instanceof NoGrouping) && !availableGroupings.isEmpty())) {
      items.setAll(baseItems);
    }
    else {
      items.clear();

      Map<Comparable<Object>, List<Object>> groupedElements = group(baseItems, order.grouper);
      Comparator<Entry<Comparable<Object>, List<Object>>> comparator = Comparator.comparing(Map.Entry::getKey);

      if(order.reverseGroupOrder) {
        comparator = comparator.reversed();
      }

      List<Entry<Comparable<Object>, List<Object>>> list = groupedElements.entrySet().stream()
        .sorted(comparator)
        .collect(Collectors.toList());

      List<Group> newGroups = new ArrayList<>();
      int position = 0;

      for(Entry<Comparable<Object>, List<Object>> e : list) {
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

  public void selectItem(Object item) {
    internalSelectedItem.setValue(item);
  }

  private Predicate<Object> createFilterPredicate() {
    Predicate<T> predicate = filter.getValue().predicate;

    if(stateFilter.getValue() != null) {
      predicate = predicate.and(stateFilter.getValue().predicate);
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
        Object itemToSelect = findBestItemToSelect();

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
      if(Objects.equals(contextItem.getValue(), rootContextItem)) {
        availableGroupings.setAll(originalGroupings);
        setRawBaseItems(rootItems);

        selectItem(oldContextItem);
      }
      else {
        @SuppressWarnings("unchecked")
        List<Object> children = (List<Object>)((Parent<T>)contextItem.getValue()).getChildren();

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
  private Object findBestItemToSelect() {
    Predicate<Object> predicate = createFilterPredicate();
    Object item = selectedItem.getValue();

    // Find an item as close as possible to the current selected item (including
    // itself) based on the current sort order:

    int previousIndex = baseItems.indexOf(item);
    int nextIndex = previousIndex + 1;  // Causes it to prefer selecting an item with a higher index when distance to a lower or higher matching item is equal

    while(previousIndex >= 0 || nextIndex < baseItems.size()) {
      if(previousIndex >= 0) {
        Object candidate = baseItems.get(previousIndex);

        if(predicate.test(candidate)) {
          return candidate;
        }
      }

      if(nextIndex < baseItems.size()) {
        Object candidate = baseItems.get(nextIndex);

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

  public static class SortOrder<T> {
    public final String resourceKey;
    public final Comparator<T> comparator;
    public final Function<Object, List<Comparable<Object>>> grouper;
    public final boolean reverseGroupOrder;

    @SuppressWarnings("unchecked")
    public <G extends Comparable<G>> SortOrder(String resourceKey, Comparator<T> comparator, Function<T, List<G>> grouper, boolean reverseGroupOrder) {
      if(comparator == null) {
        throw new IllegalArgumentException("comparator cannot be null");
      }

      this.resourceKey = resourceKey;
      this.comparator = comparator;
      this.grouper = (Function<Object, List<Comparable<Object>>>)(Function<?, ?>)grouper;
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

  public static class ViewOptions<T> {
    final List<SortOrder<T>> sortOrders;
    final List<Filter<T>> filters;
    final List<Filter<T>> stateFilters;
    final List<Grouping<T>> groupings;

    public ViewOptions(List<SortOrder<T>> sortOrders, List<Filter<T>> filters, List<Filter<T>> stateFilters, List<Grouping<T>> groupings) {
      this.sortOrders = sortOrders;
      this.filters = filters;
      this.stateFilters = stateFilters;
      this.groupings = groupings;
    }

    public ViewOptions(List<SortOrder<T>> sortOrders, List<Filter<T>> filters, List<Filter<T>> stateFilters) {
      this(sortOrders, filters, stateFilters, List.of(new NoGrouping<T>()));
    }
  }

  class HierarchicalPredicate implements Predicate<Object> {
    private final Predicate<T> predicate;

    HierarchicalPredicate(Predicate<T> predicate) {
      this.predicate = predicate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean test(Object item) {
      if(item instanceof Parent) {
        List<T> children = ((Parent<T>)item).getChildren();

        if(!children.isEmpty()) {  // usually all items implement Parent, so also check if there are children
          for(T child : children) {
            if(predicate.test(child)) {
              return true;
            }
          }

          return false;
        }
      }

      return predicate.test((T)item);
    }
  }

  class HierachicalComparator implements Comparator<Object> {
    private final Comparator<T> comparator;

    public HierachicalComparator(Comparator<T> comparator) {
      this.comparator = comparator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compare(Object a, Object b) {
      if(a instanceof Parent) {  // usually all items implement Parent, so also check if there are children
        List<T> children = ((Parent<T>)a).getChildren();

        if(!children.isEmpty()) {
          a = children.get(0);
        }
      }
      if(b instanceof Parent) {
        List<T> children = ((Parent<T>)b).getChildren();

        if(!children.isEmpty()) {
          b = children.get(0);
        }
      }

      return comparator.compare((T)a, (T)b);
    }
  }
}
