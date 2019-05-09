package hs.mediasystem.util.javafx.control;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;

import javafx.animation.AnimationTimer;
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
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

public class GridListViewSkin implements Skin<ListView<?>> {
  public final IntegerProperty visibleColumns = new SimpleIntegerProperty(4);
  public final IntegerProperty visibleRows = new SimpleIntegerProperty(3);
  public final ObjectProperty<List<Integer>> jumpPoints = new SimpleObjectProperty<>();
  public final BooleanProperty scrollBarVisible = new SimpleBooleanProperty(true);

  private final DoubleProperty scrollPosition = new SimpleDoubleProperty();  // In rows
  private final ArrayDeque<ListCell<?>> cells = new ArrayDeque<>();

  private int firstIndexInDeque;

  private ListView<?> skinnable;
  private int firstVisibleIndex;
  private int pagesToCache = 1;

  private ScrollBar scrollBar = new ScrollBar();
  private BorderPane skin;
  private Region content;

  public GridListViewSkin(ListView<?> skinnable) {
    this.skinnable = skinnable;

    getSkinnable().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
    getSkinnable().getSelectionModel().selectedIndexProperty().addListener(this::animateWhenSelectedChanges);

    skinnable.getSelectionModel().selectedItemProperty().addListener(obs -> skin.requestLayout());   // Calls layout when focused cell changes (to make sure it is at the top)

    this.content = new Region() {
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
        boolean vertical = getSkinnable().getOrientation() == Orientation.VERTICAL;
        int lines = vertical ? visibleColumns.get() : visibleRows.get();
        int firstIndex = (int)(scrollPosition.get()) * lines;

        manageCells(firstIndex);

        Insets insets = getSkinnable().getInsets();

        double w = getSkinnable().getWidth() - insets.getLeft() - insets.getRight();
        double h = getSkinnable().getHeight() - insets.getTop() - insets.getBottom();

        int cellWidth = (int)(w / visibleColumns.get());
        int cellHeight = (int)(h / visibleRows.get());
        ListCell<?> focusedCell = null;

        getChildren().clear();

        int index = firstIndexInDeque;
        setClip(new Rectangle(0, 0, getSkinnable().getWidth(), getSkinnable().getHeight()));  // Needed to clip off cells while scrolling

        if(vertical) {
          double y = (-scrollPosition.get() * cellHeight - cellHeight) % cellHeight + insets.getTop();

          for(ListCell<?> cell : cells) {
            if(index >= firstIndex) {
              int column = index % visibleColumns.get();

              if(cell.isFocused()) {
                focusedCell = cell;
              }
              else {
                getChildren().add(cell);
              }

              layoutInArea(cell, column * cellWidth + insets.getLeft(), y, cellWidth, cellHeight, 0, cell.getInsets(), true, true, HPos.CENTER, VPos.CENTER);

              if(column == visibleColumns.get() - 1) {
                y += cellHeight;

                if(y >= h) {
                  break;
                }
              }
            }

            index++;
          }
        }
        else {
          double x = (-scrollPosition.get() * cellWidth - cellWidth) % cellWidth + insets.getLeft();

          for(ListCell<?> cell : cells) {
            if(index >= firstIndex) {
              int row = index % visibleRows.get();

              if(cell.isFocused()) {
                focusedCell = cell;
              }
              else {
                getChildren().add(cell);
              }

              layoutInArea(cell, x, row * cellHeight + insets.getTop(), cellWidth, cellHeight, 0, cell.getInsets(), true, true, HPos.CENTER, VPos.CENTER);

              if(row == visibleRows.get() - 1) {
                x += cellWidth;

                if(x >= w) {
                  break;
                }
              }
            }

            index++;
          }
        }

        if(focusedCell != null) {
          getChildren().add(focusedCell);
        }
      }

      private void manageCells(int firstIndex) {
        int cellsPerPage = visibleColumns.get() * visibleRows.get();
        int firstIndexToCache = firstIndex - pagesToCache * cellsPerPage;
        int requiredFirstIndexInDeque = Math.max(0, firstIndexToCache);
        int requiredLastIndexInDeque = Math.min(firstIndexToCache + (pagesToCache * 2 + 1) * cellsPerPage, getSkinnable().getItems().size());  // exclusive

        // Add cells to start of queue if needed
        while(firstIndexInDeque > requiredFirstIndexInDeque) {
          ListCell<?> cell = createCell();

          cell.updateIndex(--firstIndexInDeque);
          cells.addFirst(cell);
        }

        // Remove cells from front of queue if needed:
        while(firstIndexInDeque < requiredFirstIndexInDeque && !cells.isEmpty()) {
          cells.removeFirst().updateIndex(-1);
          firstIndexInDeque++;
        }

        firstIndexInDeque = requiredFirstIndexInDeque;  // Sync index in case all cells were removed

        // Add cells to end of queue if needed:
        while(firstIndexInDeque + cells.size() < requiredLastIndexInDeque) {
          ListCell<?> cell = createCell();

          cell.updateIndex(firstIndexInDeque + cells.size());
          cells.addLast(cell);
        }

        // Remove cells from end of queue if needed:
        while(firstIndexInDeque + cells.size() > requiredLastIndexInDeque && !cells.isEmpty()) {
          cells.removeLast().updateIndex(-1);
        }
      }

      private ListCell<?> createCell() {
        @SuppressWarnings("unchecked")
        ListView<Object> listView = (ListView<Object>)getSkinnable();
        ListCell<Object> cell = listView.getCellFactory().call(listView);

        cell.updateListView(listView);

        return cell;
      }
    };

    scrollBar.orientationProperty().bind(skinnable.orientationProperty());
    getSkinnable().orientationProperty().addListener(obs -> relayout());
    scrollBarVisible.addListener(obs -> relayout());

    visibleRows.addListener(obs -> content.requestLayout());
    visibleColumns.addListener(obs -> content.requestLayout());
    scrollPosition.addListener(obs -> content.requestLayout());    // Calls layout when scroll position is updated for animation

    InvalidationListener updateScrollBarListener = this::updateScrollBar;

    scrollPosition.addListener(updateScrollBarListener);
    getSkinnable().getItems().addListener(updateScrollBarListener);
    getSkinnable().getItems().addListener((Observable obs) -> content.requestLayout());  // When filter is removed, new items may appear/disappear without selected index changing, so must update in that case as well

    getSkinnable().itemsProperty().addListener((obs, old, current) -> {
      if(old != null) {
        old.removeListener(updateScrollBarListener);
      }
      if(current != null) {
        current.addListener(updateScrollBarListener);
        updateScrollBar(obs);
      }
    });

    skin = new BorderPane();

    relayout();
  }

  private void relayout() {
    skin.setBottom(null);
    skin.setRight(null);
    skin.setCenter(content);

    if(scrollBarVisible.get()) {
      if(getSkinnable().getOrientation() == Orientation.VERTICAL) {
        skin.setRight(scrollBar);
      }
      else {
        skin.setBottom(scrollBar);
      }
    }

    content.requestLayout();
  }

  private void updateScrollBar(@SuppressWarnings("unused") Observable obs) {
    int lineCount = getSkinnable().getOrientation() == Orientation.VERTICAL ? getRowCount() : getColumnCount();
    int visibleLines = getSkinnable().getOrientation() == Orientation.VERTICAL ? visibleRows.get() : visibleColumns.get();
    List<Integer> jumpPoints = this.jumpPoints.get();

    if(jumpPoints != null) {
      // Adjust line count in case jump point is near end:
      int lastJumpPoint = jumpPoints.get(jumpPoints.size() - 1);
      int totalItems = getSkinnable().getItems().size();
      int itemsVisibleAtLastJumpPoint = totalItems - lastJumpPoint;
      int perpendicularLines = getSkinnable().getOrientation() == Orientation.VERTICAL ? visibleColumns.get() : visibleRows.get();
      int linesOccupiedAtLastJumpPoint = (itemsVisibleAtLastJumpPoint + perpendicularLines - 1) / perpendicularLines;

      //System.out.println("itemsVis = " + itemsVisibleAtLastJumpPoint + "; linesOccu = " + linesOccupiedAtLastJumpPoint + "; x = " + perpendicularLines);
      if(linesOccupiedAtLastJumpPoint < visibleLines) {
        lineCount += visibleLines - linesOccupiedAtLastJumpPoint;
      }
    }

    int max = Math.max(0, lineCount - visibleLines);

    scrollBar.setMin(0);
    scrollBar.setMax(max);
    scrollBar.setValue(scrollPosition.get());
    scrollBar.setVisibleAmount((double)visibleLines / lineCount * max);
  }

  private int getColumnCount() {
    return (getSkinnable().getItems().size() + visibleRows.get() - 1) / visibleRows.get();
  }

  private int getRowCount() {
    return (getSkinnable().getItems().size() + visibleColumns.get() - 1) / visibleColumns.get();
  }

  private AnimationTimer animationTimer = new AnimationTimer() {
    private long lastUpdate;
    private boolean active;

    @Override
    public void handle(long now) {
      double pos = scrollPosition.get();
      double targetPos = firstVisibleIndex / (getSkinnable().getOrientation() == Orientation.VERTICAL ? visibleColumns.get() : visibleRows.get());

      if(lastUpdate != 0) {
        long dt = now - lastUpdate;

        if(pos != targetPos) {
          // target rate should be something like 1% of distance per 4ms
          // ..but not smaller than 1/20th of a row
          // ..but not bigger than the distance still left

          //double x = 400_000_000.0 / dt;
          double scrollFraction = dt * 0.000000005;  // fraction = 0.000000005/ns = 0.005/ms = 5/sec = 500% of distance scrolled/sec
          double sign = pos < targetPos ? 1 : -1;
          double distance = Math.abs(targetPos - pos);
          double delta = distance * Math.pow(scrollFraction, 0.6);

          delta = Math.min(Math.max(delta, 1.0 / 50), distance);

//          System.out.println("pos = " + pos + ", firstVisibleIndex = " + firstVisibleIndex + ", target = " + targetPos + ", delta = " + delta + ", distance = " + distance + ", fraction = " + scrollFraction + ", dt = " + dt);

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
    int visibleItems = visibleColumns.get() * visibleRows.get();

    if(selectedIndex >= firstVisibleIndex + visibleItems || selectedIndex < firstVisibleIndex) {
      if(getSkinnable().getOrientation() == Orientation.VERTICAL) {
        int maxVisibleIndex = Math.max(getSkinnable().getItems().size(), (getRowCount() - visibleRows.get()) * visibleColumns.get());  // ensures a full page will be visible at bottom of list

        if(selectedIndex < firstVisibleIndex) {
          firstVisibleIndex = Math.min(maxVisibleIndex, selectedIndex - (selectedIndex % visibleColumns.get()));
        }
        else {
          firstVisibleIndex = Math.min(maxVisibleIndex, selectedIndex - (selectedIndex % visibleColumns.get()) - visibleColumns.get() * (visibleRows.get() - 1));
        }
      }
      else {
        int maxVisibleIndex = Math.max(getSkinnable().getItems().size(), (getColumnCount() - visibleColumns.get()) * visibleRows.get());  // ensures a full page will be visible at bottom of list

        if(selectedIndex < firstVisibleIndex) {
          firstVisibleIndex = Math.min(maxVisibleIndex, selectedIndex - (selectedIndex % visibleRows.get()));
        }
        else {
          firstVisibleIndex = Math.min(maxVisibleIndex, selectedIndex - (selectedIndex % visibleRows.get()) - visibleRows.get() * (visibleColumns.get() - 1));
        }
      }

      // Adjust firstVisibleIndex so a jump point is not visible:
      if(jumpPoints.get() != null) {
        int index = Collections.binarySearch(jumpPoints.get(), firstVisibleIndex);

        if(index < 0) {
          index = -index - 1;
        }

        if(index < jumpPoints.get().size()) {
          int jumpPoint = jumpPoints.get().get(index);

          if(selectedIndex < jumpPoint) {
            firstVisibleIndex = Math.min(firstVisibleIndex, jumpPoint - visibleItems);
          }
          else {
            firstVisibleIndex = Math.max(firstVisibleIndex, jumpPoint);
          }
        }
      }

      animationTimer.start();
//      if(scrollTimeline != null) {
//        scrollTimeline.stop();
//      }
//
//      scrollTimeline = new Timeline(
//        new KeyFrame(Duration.ZERO, new KeyValue(scrollPosition, scrollPosition.get())),
//        new KeyFrame(Duration.seconds(0.3), new KeyValue(scrollPosition, firstVisibleIndex / visibleColumns.get(), Interpolator.LINEAR))
//      );
//
//      scrollTimeline.play();
    }

    System.out.println(">>> SELECTED " + selectedIndex + "... first=" + firstVisibleIndex + "; scrollp = " + scrollPosition.get() + "; first=" + firstTime);

    if(firstTime) {
      scrollPosition.set(firstVisibleIndex / (getSkinnable().getOrientation() == Orientation.VERTICAL ? visibleColumns.get() : visibleRows.get()));
      firstTime = false;
    }
  }

  private void handleKeyEvent(KeyEvent e) {
    int selectedIndex = getSkinnable().getSelectionModel().getSelectedIndex();

    if(e.getCode().isNavigationKey()) {
      int rowSize = getSkinnable().getOrientation() == Orientation.VERTICAL ? visibleColumns.get() : 1;
      int columnSize = getSkinnable().getOrientation() == Orientation.VERTICAL ? 1 : visibleRows.get();

      if(KeyCode.LEFT == e.getCode()) {
        selectedIndex -= columnSize;
      }
      else if(KeyCode.RIGHT == e.getCode()) {
        selectedIndex += columnSize;
      }
      else if(KeyCode.UP == e.getCode()) {
        selectedIndex -= rowSize;
      }
      else if(KeyCode.DOWN == e.getCode()) {
        selectedIndex += rowSize;
      }
      else if(KeyCode.HOME == e.getCode()) {
        selectedIndex = 0;
      }
      else if(KeyCode.END == e.getCode()) {
        selectedIndex = getSkinnable().getItems().size() - 1;
      }
      else if(KeyCode.PAGE_UP == e.getCode()) {
        if(jumpPoints.get() != null) {
          int index = Collections.binarySearch(jumpPoints.get(), selectedIndex);

          if(index < 0) {
            index = -index - 1;
          }

          index--;

          if(index < 0) {
            index = 0;
          }

          selectedIndex = jumpPoints.get().get(index);
        }
        else {
          selectedIndex -= visibleColumns.get() * visibleRows.get();
        }
      }
      else if(KeyCode.PAGE_DOWN == e.getCode()) {
        if(jumpPoints.get() != null) {
          int index = Collections.binarySearch(jumpPoints.get(), selectedIndex);

          if(index < 0) {
            index = -index - 1;
          }
          else {
            index++;
          }

          if(index < jumpPoints.get().size()) {
            selectedIndex = jumpPoints.get().get(index);
          }
        }
        else {
          selectedIndex += visibleColumns.get() * visibleRows.get();
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
}
