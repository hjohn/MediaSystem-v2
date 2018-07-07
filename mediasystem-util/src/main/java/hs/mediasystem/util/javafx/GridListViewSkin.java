package hs.mediasystem.util.javafx;

import java.util.ArrayDeque;

import javafx.animation.AnimationTimer;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
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

  private final DoubleProperty scrollPosition = new SimpleDoubleProperty();  // In rows
  private final ArrayDeque<ListCell<?>> cells = new ArrayDeque<>();

  private int firstIndexInDeque;

  private ListView<?> skinnable;
  private int firstVisibleIndex;
  private int pagesToCache = 1;

  private ScrollBar scrollBar = new ScrollBar();
  private BorderPane skin;

  public GridListViewSkin(ListView<?> skinnable) {
    this.skinnable = skinnable;

    getSkinnable().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
    getSkinnable().getSelectionModel().selectedIndexProperty().addListener(this::animateWhenSelectedChanges);

    skinnable.getSelectionModel().selectedItemProperty().addListener(obs -> skin.requestLayout());   // Calls layout when focused cell changes (to make sure it is at the top)

    Region content = new Region() {
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
        int firstIndex = (int)(scrollPosition.get()) * visibleColumns.get();
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

        Insets insets = getSkinnable().getInsets();

        double w = getSkinnable().getWidth() - insets.getLeft() - insets.getRight();
        double h = getSkinnable().getHeight() - insets.getTop() - insets.getBottom();

        int cellWidth = (int)(w / visibleColumns.get());
        int cellHeight = (int)(h / visibleRows.get());

        double y = (-scrollPosition.get() * cellHeight - cellHeight) % cellHeight + insets.getTop();

        getChildren().clear();

        int index = firstIndexInDeque;
        setClip(new Rectangle(0, 0, getSkinnable().getWidth(), getSkinnable().getHeight()));  // Needed to clip off cells while scrolling

        ListCell<?> focusedCell = null;

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

        if(focusedCell != null) {
          getChildren().add(focusedCell);
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

    scrollBar.setOrientation(skinnable.getOrientation());

    visibleRows.addListener(obs -> content.requestLayout());
    visibleColumns.addListener(obs -> content.requestLayout());
    scrollPosition.addListener(obs -> content.requestLayout());    // Calls layout when scroll position is updated for animation

    InvalidationListener updateScrollBarListener = this::updateScrollBar;

    scrollPosition.addListener(updateScrollBarListener);
    getSkinnable().getItems().addListener(updateScrollBarListener);

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

    skin.setCenter(content);
    skin.setRight(scrollBar);
  }

  private void updateScrollBar(@SuppressWarnings("unused") Observable obs) {
    int rowCount = getRowCount();
    int max = Math.max(0, rowCount - visibleRows.get());

    scrollBar.setMin(0);
    scrollBar.setMax(max);
    scrollBar.setValue(scrollPosition.get());
    scrollBar.setVisibleAmount((double)visibleRows.get() / rowCount * max);
  }

  private int getRowCount() {
    return (getSkinnable().getItems().size() + visibleColumns.get() - 1) / visibleColumns.get();
  }

  private AnimationTimer animationTimer = new AnimationTimer() {
    private long lastUpdate;
    private boolean active;

    @Override
    public void handle(long now) {
      if(lastUpdate != 0) {
        double pos = scrollPosition.get();
        double targetPos = firstVisibleIndex / visibleColumns.get();
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

          System.out.println("pos = " + pos + ", firstVisibleIndex = " + firstVisibleIndex + ", target = " + targetPos + ", delta = " + delta + ", distance = " + distance + ", fraction = " + scrollFraction + ", dt = " + dt);

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

  private void animateWhenSelectedChanges(@SuppressWarnings("unused") Observable obs) {
    int selectedIndex = getSkinnable().getSelectionModel().getSelectedIndex();

    if(selectedIndex >= firstVisibleIndex + visibleColumns.get() * visibleRows.get() || selectedIndex < firstVisibleIndex) {
      int maxVisibleIndex = Math.max(getSkinnable().getItems().size(), (getRowCount() - visibleRows.get()) * visibleColumns.get());  // ensures a full page will be visible at bottom of list

      if(selectedIndex < firstVisibleIndex) {
        firstVisibleIndex = Math.min(maxVisibleIndex, selectedIndex - (selectedIndex % visibleColumns.get()));
      }
      else {
        firstVisibleIndex = Math.min(maxVisibleIndex, selectedIndex - (selectedIndex % visibleColumns.get()) - visibleColumns.get() * (visibleRows.get() - 1));
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
  }

  private void handleKeyEvent(KeyEvent e) {
    int selectedIndex = getSkinnable().getSelectionModel().getSelectedIndex();

    if(e.getCode().isNavigationKey()) {
      if(KeyCode.LEFT == e.getCode()) {
        selectedIndex--;
      }
      else if(KeyCode.RIGHT == e.getCode()) {
        selectedIndex++;
      }
      else if(KeyCode.UP == e.getCode()) {
        selectedIndex -= visibleColumns.get();
      }
      else if(KeyCode.DOWN == e.getCode()) {
        selectedIndex += visibleColumns.get();
      }
      else if(KeyCode.HOME == e.getCode()) {
        selectedIndex = 0;
      }
      else if(KeyCode.END == e.getCode()) {
        selectedIndex = getSkinnable().getItems().size() - 1;
      }
      else if(KeyCode.PAGE_UP == e.getCode()) {
        selectedIndex -= visibleColumns.get() * visibleRows.get();
      }
      else if(KeyCode.PAGE_DOWN == e.getCode()) {
        selectedIndex += visibleColumns.get() * visibleRows.get();
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
