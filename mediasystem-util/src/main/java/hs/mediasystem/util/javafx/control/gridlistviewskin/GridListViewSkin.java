package hs.mediasystem.util.javafx.control.gridlistviewskin;

import hs.mediasystem.util.javafx.control.VerticalLabel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class GridListViewSkin implements Skin<ListView<?>> {
  public final IntegerProperty visibleColumns = new SimpleIntegerProperty(4);
  public final IntegerProperty visibleRows = new SimpleIntegerProperty(3);
  public final BooleanProperty scrollBarVisible = new SimpleBooleanProperty(true);
  public final ObjectProperty<Pos> cellAlignment = new SimpleObjectProperty<>(Pos.CENTER);

  /**
   * Defines where groups start and their title, leave <code>null</code> for no
   * groups.
   */
  public final ObjectProperty<List<Group>> groups = new SimpleObjectProperty<>();

  /**
   * Show the headers in between groups.
   */
  public final BooleanProperty showHeaders = new SimpleBooleanProperty(true);

  /**
   * When true, skipping to the next page will instead skip to the end of the
   * current group, or to the start of the next group if already at the end.
   * Skipping to the previous page will skip to the start of the current group
   * or to the start of the previous group if already at the start.
   */
  public final BooleanProperty pageByGroup = new SimpleBooleanProperty(true);

  public enum GroupDisplayMode {

    /**
     * No special scrolling is performed to display the current group.
     */
    NONE,

    /**
     * Scrolls as much as possible of the current group into view when switching
     * groups.
     */
    FOCUSED
  }

  /**
   * The {@link GroupDisplayMode}.
   */
  public final ObjectProperty<GroupDisplayMode> groupDisplayMode = new SimpleObjectProperty<>(GroupDisplayMode.NONE);

  private final DoubleProperty scrollPosition = new SimpleDoubleProperty();  // In rows
  private final ArrayDeque<ListCell<?>> cells = new ArrayDeque<>();

  private int firstIndexInDeque;

  private ListView<?> skinnable;
  private int firstVisibleIndex;
  private int pagesToCache = 1;

  private ScrollBar scrollBar = new ScrollBar();
  private BorderPane skin;
  private Region content;

  private GroupManager gm;
  private boolean vertical;

  public GridListViewSkin(ListView<?> skinnable) {
    this.skinnable = skinnable;

    skinnable.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
    skinnable.getSelectionModel().selectedIndexProperty().addListener(this::animateWhenSelectedChanges);
    skinnable.getSelectionModel().selectedItemProperty().addListener(obs -> skin.requestLayout());   // Calls layout when focused cell changes (to make sure it is at the top)

    this.content = new Region() {
      private final HeaderRegion groupHeaderPane = new HeaderRegion();

      private Rectangle clip = null;

      {
        groupHeaderPane.setMouseTransparent(true);

        getChildren().add(groupHeaderPane);

        groupHeaderPane.setViewOrder(-1);
      }

      @Override
      protected double computeMinWidth(double height) {
        return 200;
      }

      @Override
      protected double computeMinHeight(double width) {
        return 200;
      }

      @Override
      protected double computePrefWidth(double height) {
        return 400;
      }

      @Override
      protected double computePrefHeight(double width) {
        return 400;
      }

      @Override
      protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
      }

      @Override
      protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
      }

      @Override
      protected void layoutChildren() {
        int lines = vertical ? visibleColumns.get() : visibleRows.get();
        int firstIndex = (int)(scrollPosition.get()) * lines;

        manageCells(firstIndex);

        Insets insets = getSkinnable().getInsets();

        double w = getSkinnable().getWidth() - insets.getLeft() - insets.getRight();
        double h = getSkinnable().getHeight() - insets.getTop() - insets.getBottom();

        int cellWidth = (int)(w / visibleColumns.get());
        int cellHeight = (int)(h / visibleRows.get());
        int index = firstIndexInDeque;

        if(clip == null || clip.getWidth() != getSkinnable().getWidth() || clip.getHeight() != getSkinnable().getHeight()) {
          this.clip = new Rectangle(0, 0, getSkinnable().getWidth(), getSkinnable().getHeight());

          setClip(clip);  // Needed to clip off cells while scrolling
          layoutInArea(groupHeaderPane, 0, 0, getSkinnable().getWidth(), getSkinnable().getHeight(), 0, Insets.EMPTY, true, true, cellAlignment.get().getHpos(), cellAlignment.get().getVpos());
        }

        groupHeaderPane.layoutChildren();

        for(ListCell<?> cell : cells) {
          int viewIndex = gm.toViewIndex(index++);

          cell.setVisible(false);

          if(viewIndex >= firstIndex) {
            if(vertical) {
              int column = viewIndex % visibleColumns.get();
              int row = viewIndex / visibleColumns.get();

              double y = ((row - scrollPosition.get()) * cellHeight) + insets.getTop();

              if(y < getSkinnable().getHeight()) {
                cell.setVisible(true);
                layoutInArea(cell, column * cellWidth + insets.getLeft(), y, cellWidth, cellHeight, 0, cell.getInsets(), true, true, cellAlignment.get().getHpos(), cellAlignment.get().getVpos());
              }
            }
            else {
              int column = viewIndex / visibleRows.get();
              int row = viewIndex % visibleRows.get();

              double x = ((column - scrollPosition.get()) * cellWidth) + insets.getLeft();

              if(x < w) {
                cell.setVisible(true);
                layoutInArea(cell, x, row * cellHeight + insets.getTop(), cellWidth, cellHeight, 0, cell.getInsets(), true, true, HPos.CENTER, VPos.CENTER);
              }
            }
          }
        }
      }

      private void manageCells(int firstIndex) {  // this is a view index
        int cellsPerPage = visibleColumns.get() * visibleRows.get();
        int firstIndexToCache = gm.toModelIndexSmart(firstIndex - pagesToCache * cellsPerPage);
        int requiredFirstIndexInDeque = Math.max(0, firstIndexToCache);
        int requiredLastIndexInDeque = Math.min(firstIndexToCache + (pagesToCache * 2 + 1) * cellsPerPage, getSkinnable().getItems().size());  // exclusive

        /*
         * To avoid creating new cells, always first remove cells that are not part of the new
         * range before creating new ones.
         */

        // Remove cells from front of queue if needed:
        while(firstIndexInDeque < requiredFirstIndexInDeque && !cells.isEmpty()) {
          recycleCell(cells.removeFirst());
          firstIndexInDeque++;
        }

        // Remove cells from end of queue if needed:
        while(firstIndexInDeque + cells.size() > requiredLastIndexInDeque && !cells.isEmpty()) {
          recycleCell(cells.removeLast());
        }

        /*
         * If all cells were recycled, there was no overlap at all.  In this special
         * case firstIndexInDeque will be reset to requiredFirstIndexInDeque.
         */

        if(cells.isEmpty()) {
          GridListViewSkin.this.firstIndexInDeque = requiredFirstIndexInDeque;
        }

        // Add cells to start of queue if needed
        while(firstIndexInDeque > requiredFirstIndexInDeque) {
          ListCell<?> cell = createCell();

          cell.updateIndex(--firstIndexInDeque);
          cells.addFirst(cell);
        }

        // Add cells to end of queue if needed:
        while(requiredFirstIndexInDeque + cells.size() < requiredLastIndexInDeque) {
          ListCell<?> cell = createCell();

          cell.updateIndex(requiredFirstIndexInDeque + cells.size());
          cells.addLast(cell);
        }
      }

      private final List<ListCell<?>> recycledCells = new ArrayList<>();

      private void recycleCell(ListCell<?> cell) {
        recycledCells.add(cell);
        cell.setVisible(false);  // Needed because cells on recycled list are still part of Children, but will not be made invisible in layoutChildren as they're not part of cells list
      }

      private ListCell<?> createCell() {
        if(!recycledCells.isEmpty()) {
          return recycledCells.remove(recycledCells.size() - 1);
        }

        @SuppressWarnings("unchecked")
        ListView<Object> listView = (ListView<Object>)getSkinnable();
        ListCell<Object> cell = listView.getCellFactory().call(listView);

        cell.updateListView(listView);
        getChildren().add(cell);

        return cell;
      }
    };

    scrollBar.orientationProperty().bind(skinnable.orientationProperty());
    skinnable.orientationProperty().addListener(obs -> relayout());
    scrollBarVisible.addListener(obs -> relayout());

    visibleRows.addListener(obs -> content.requestLayout());
    visibleColumns.addListener(obs -> content.requestLayout());
    scrollPosition.addListener(obs -> content.requestLayout());    // Calls layout when scroll position is updated for animation
    cellAlignment.addListener(obs -> content.requestLayout());

    InvalidationListener updateScrollBarListener = this::updateScrollBar;

    scrollPosition.addListener(updateScrollBarListener);
    skinnable.getItems().addListener(updateScrollBarListener);
    skinnable.getItems().addListener((Observable obs) -> content.requestLayout());  // When filter is removed, new items may appear/disappear without selected index changing, so must update in that case as well

    skinnable.itemsProperty().addListener((obs, old, current) -> {
      if(old != null) {
        old.removeListener(updateScrollBarListener);
      }
      if(current != null) {
        current.addListener(updateScrollBarListener);
        updateScrollBar(obs);
      }
    });

    skinnable.orientationProperty().addListener(obs -> updateProperties());
    visibleColumns.addListener(obs -> updateProperties());
    visibleRows.addListener(obs -> updateProperties());
    groups.addListener(obs -> updateProperties());

    skin = new BorderPane();

    updateProperties();
    relayout();
  }

  private void updateProperties() {
    vertical = skinnable.getOrientation() == Orientation.VERTICAL;
    gm = new GroupManager(
      groups.get() == null ? new int[] {} : groups.get().stream().mapToInt(Group::getPosition).toArray(),
      vertical ? visibleColumns.get() : visibleRows.get()
    );
    animateWhenSelectedChanges(null);
  }

  private void relayout() {
    skin.setBottom(null);
    skin.setRight(null);
    skin.setCenter(content);

    if(scrollBarVisible.get()) {
      if(vertical) {
        skin.setRight(scrollBar);
      }
      else {
        skin.setBottom(scrollBar);
      }
    }

    content.requestLayout();
  }

  private void updateScrollBar(@SuppressWarnings("unused") Observable obs) {
    int lineCount = gm.getViewRowNumber(skinnable.getItems().size() - 1) + 1;
    int visibleLines = vertical ? visibleRows.get() : visibleColumns.get();
    List<Group> groups = this.groups.get();

    if(groups != null) {
      // Adjust line count in case last group is near end:
      int lastGroupIndex = groups.get(groups.size() - 1).getPosition();  // TODO rare cases get IndexOutOfBoundsException here (-1, length = 0)
      int totalItems = getSkinnable().getItems().size();
      int itemsVisibleInLastGroup = totalItems - lastGroupIndex;
      int linesOccupiedInLastGroup = (itemsVisibleInLastGroup + gm.getWidth() - 1) / gm.getWidth();

      if(linesOccupiedInLastGroup < visibleLines) {
        lineCount += visibleLines - linesOccupiedInLastGroup;
      }
    }

    int max = Math.max(0, lineCount - visibleLines);

    scrollBar.setMin(0);
    scrollBar.setMax(max);
    scrollBar.setValue(scrollPosition.get());
    scrollBar.setVisibleAmount((double)visibleLines / lineCount * max);
  }

  private AnimationTimer animationTimer = new AnimationTimer() {
    private long lastUpdate;
    private boolean active;

    @Override
    public void handle(long now) {
      double pos = scrollPosition.get();
      double targetPos = gm.getViewRowNumber(firstVisibleIndex);

      if(lastUpdate != 0) {
        long dt = now - lastUpdate;

        if(pos != targetPos) {

          /*
           * Target scroll rate is intended to be something like 1% of the distance to
           * scroll per frame (4ms), but at least 1/50th of a row, but no bigger than
           * the distance still left.
           */

          double scrollFraction = dt * 0.000000005;  // fraction = 0.000000005/ns = 0.005/ms = 5/sec = 500% of distance scrolled/sec
          double sign = pos < targetPos ? 1 : -1;
          double distance = Math.abs(targetPos - pos);
          double delta = distance * Math.pow(scrollFraction, 0.6);

          delta = Math.min(Math.max(delta, 1.0 / 50), distance);

          scrollPosition.set(pos + delta * sign);
        }
        else {
          this.stop();
        }
      }

      lastUpdate = now;
    }

    @Override
    public void stop() {
      if(active) {
        active = false;

        super.stop();
      }
    }

    @Override
    public void start() {
      if(!active) {
        lastUpdate = 0;
        active = true;

        super.start();
      }
    }
  };

  private boolean firstTime = true;

  private void animateWhenSelectedChanges(@SuppressWarnings("unused") Observable obs) {
    int selectedIndex = getSkinnable().getSelectionModel().getSelectedIndex();
    int length = vertical ? visibleRows.get() : visibleColumns.get();
    int visibleItems = gm.countViewItems(firstVisibleIndex, length);
    int originalFirstVisibleIndex = firstVisibleIndex;
    boolean scrollBackwards = selectedIndex < firstVisibleIndex;

    if(selectedIndex >= firstVisibleIndex + visibleItems || scrollBackwards) {  // Is new selected index outside visual area?
      int maxLineIndex = gm.getViewRowNumber(skinnable.getItems().size() - 1);

      int maxViewIndex = Math.max(0, gm.modelIndexOfViewRow(maxLineIndex - length + 1));  // ensures a full page will be visible at bottom of list
      int viewRowNumber = gm.getViewRowNumber(selectedIndex);

      firstVisibleIndex = Math.min(
        maxViewIndex,
        gm.modelIndexOfViewRow(viewRowNumber - (scrollBackwards ? 0 : length - 1))
      );
    }

    if(groups.get() != null && groupDisplayMode.get() == GroupDisplayMode.FOCUSED) {
      // First move up so next group (if any) is no longer visible:
      int selectedItemGroupIndex = gm.groupNumber(selectedIndex);

      if(selectedItemGroupIndex + 1 < groups.get().size()) {
        int nextGroupStart = gm.modelIndexOfGroup(selectedItemGroupIndex + 1);
        int startRow = gm.getViewRowNumber(nextGroupStart) - length;

        firstVisibleIndex = Math.max(0, Math.min(firstVisibleIndex, gm.modelIndexOfViewRow(startRow)));
      }

      // Then move down so first visible index is part of current group (if it is not):
      int firstVisibleGroupIndex = gm.groupNumber(firstVisibleIndex);

      if(firstVisibleGroupIndex < selectedItemGroupIndex) {
        firstVisibleIndex = gm.modelIndexOfGroup(selectedItemGroupIndex);  // Move forward so first visible index is part of same group as selected index
      }
    }

    if(originalFirstVisibleIndex != firstVisibleIndex) {
      animationTimer.start();
    }

    if(firstTime) {
      scrollPosition.set(gm.getViewRowNumber(firstVisibleIndex));

      Platform.runLater(() -> firstTime = false);
    }
  }

  private void handleKeyEvent(KeyEvent e) {
    int selectedIndex = getSkinnable().getSelectionModel().getSelectedIndex();

    if(e.getCode().isNavigationKey()) {
      int rowSize = vertical ? visibleColumns.get() : 1;
      int columnSize = vertical ? 1 : visibleRows.get();

      if(KeyCode.LEFT == e.getCode()) {
        selectedIndex = gm.toModelIndexSmart(gm.toViewIndex(selectedIndex) - columnSize);
      }
      else if(KeyCode.RIGHT == e.getCode()) {
        selectedIndex = vertical ? selectedIndex + 1 : gm.toModelIndexSmart(gm.toViewIndex(selectedIndex) + columnSize);
      }
      else if(KeyCode.UP == e.getCode()) {
        selectedIndex = gm.toModelIndexSmart(gm.toViewIndex(selectedIndex) - rowSize);
      }
      else if(KeyCode.DOWN == e.getCode()) {
        selectedIndex = vertical ? gm.toModelIndexSmart(gm.toViewIndex(selectedIndex) + rowSize) : selectedIndex + 1;
      }
      else if(KeyCode.HOME == e.getCode()) {
        selectedIndex = 0;
      }
      else if(KeyCode.END == e.getCode()) {
        selectedIndex = getSkinnable().getItems().size() - 1;
      }
      else if(KeyCode.PAGE_UP == e.getCode()) {
        if(pageByGroup.get() && groups.get() != null) {
          int currentGroupIndex = gm.groupNumber(selectedIndex);

          if(gm.modelIndexOfGroup(currentGroupIndex) == selectedIndex) {  // At first item?
            currentGroupIndex--;
          }

          if(currentGroupIndex >= 0) {
            selectedIndex = groups.get().get(currentGroupIndex).getPosition();
          }
        }
        else {
          selectedIndex = gm.toModelIndexSmart(gm.toViewIndex(selectedIndex) - visibleColumns.get() * visibleRows.get());
        }
      }
      else if(KeyCode.PAGE_DOWN == e.getCode()) {
        if(pageByGroup.get() && groups.get() != null) {
          int currentGroupIndex = gm.groupNumber(selectedIndex) + 1;

          if(currentGroupIndex < groups.get().size()) {
            selectedIndex = groups.get().get(currentGroupIndex).getPosition();
          }
          else {
            selectedIndex = skinnable.getItems().size() - 1;
          }
        }
        else {
          selectedIndex = gm.toModelIndexSmart(gm.toViewIndex(selectedIndex) + visibleColumns.get() * visibleRows.get());
        }
      }

      if(selectedIndex < 0) {
        selectedIndex = 0;
      }
      else if(selectedIndex >= getSkinnable().getItems().size()) {
        selectedIndex = getSkinnable().getItems().size() - 1;
      }

      getSkinnable().getSelectionModel().select(selectedIndex);
      e.consume();
    }
  }

  @Override
  public void dispose() {
    this.animationTimer.stop();

    this.skinnable = null;
    this.skin = null;
    this.scrollBar = null;
    this.animationTimer = null;
  }

  @Override
  public Node getNode() {
    return skin;
  }

  @Override
  public ListView<?> getSkinnable() {
    return skinnable;
  }

  private class HeaderRegion extends Region {

    @Override
    public void layoutChildren() {
      if(!showHeaders.get() || groups.get() == null) {
        getChildren().clear();

        return;
      }

      int lines = vertical ? visibleColumns.get() : visibleRows.get();
      int firstIndex = (int)(scrollPosition.get()) * lines;

      Insets insets = getSkinnable().getInsets();

      double w = getSkinnable().getWidth() - insets.getLeft() - insets.getRight();
      double h = getSkinnable().getHeight() - insets.getTop() - insets.getBottom();

      int cellWidth = (int)(w / visibleColumns.get());
      int cellHeight = (int)(h / visibleRows.get());

      List<Group> groupsList = groups.get();
      List<Node> children = new ArrayList<>(getChildren());  // copy of existing children

      for(int i = 0; i < groupsList.size(); i++) {
        int groupStartIndex = groupsList.get(i).getPosition();

        if(i + 1 >= groupsList.size() || gm.viewIndexOfGroup(i + 1) > firstIndex) {
          int viewIndex = gm.toViewIndex(groupStartIndex);

          if(vertical) {
            int row = viewIndex / visibleColumns.get();
            double y = ((row - scrollPosition.get()) * cellHeight) + insets.getTop();

            if(y < insets.getTop()) {  // Stick header to top if in the middle of a group
              y = insets.getTop();
            }

            if(y >= getSkinnable().getHeight()) {
              break;
            }

            Separator separator = children.isEmpty() ? new Separator() : (Separator)children.remove(0);

            if(separator.getParent() == null) {
              getChildren().add(separator);
            }

            separator.horizontal.set(true);
            separator.title.set(groupsList.get(i).getTitle());

            layoutInArea(separator, insets.getLeft(), y, w, cellHeight, 0, separator.getInsets(), true, false, HPos.CENTER, VPos.TOP);
          }
          else {
            int column = viewIndex / visibleRows.get();
            double x = ((column - scrollPosition.get()) * cellWidth) + insets.getLeft();

            if(x < insets.getLeft()) {  // Stick header to left if in the middle of a group
              x = insets.getLeft();
            }

            if(x >= w) {
              break;
            }

            Separator separator = children.isEmpty() ? new Separator() : (Separator)children.remove(0);

            if(separator.getParent() == null) {
              getChildren().add(separator);
            }

            separator.horizontal.set(false);
            separator.title.set(groupsList.get(i).getTitle());

            layoutInArea(separator, x, insets.getTop(), cellWidth, h, 0, separator.getInsets(), false, true, HPos.LEFT, VPos.CENTER);
          }
        }
      }

      getChildren().removeAll(children);  // remove children that weren't needed
    }
  }

  public static class Separator extends StackPane {
    public final StringProperty title = new SimpleStringProperty();
    public final BooleanProperty horizontal = new SimpleBooleanProperty();

    public Separator() {
      horizontal.addListener(obs -> updateStructure());

      updateStructure();
    }

    private void updateStructure() {
      getStyleClass().setAll("group-heading", horizontal.get() ? "horizontal" : "vertical");

      getChildren().removeIf(node -> {
        if(node instanceof Label label) {
          label.textProperty().unbind();
        }
        else if(node instanceof VerticalLabel label) {
          label.textProperty().unbind();
        }

        return true;
      });

      if(horizontal.get()) {
        Label label = new Label();

        label.textProperty().bind(title);

        getChildren().add(label);
      }
      else {
        VerticalLabel label = new VerticalLabel(VerticalDirection.UP);

        label.textProperty().bind(title);

        getChildren().add(label);
      }
    }
  }
}
