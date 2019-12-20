package hs.mediasystem.util.javafx.control.carousel;

import hs.mediasystem.util.Tuple.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

// Note: Remove all padding from ListCell; any padding creates empty space around the
//       cell which is included in the perspective transform; in combination with a
//       reflection and reflection clipping, the clip will include part of the
//       empty space making it seem the reflection is clipped wrong (only visible
//       if reflections overlap).

public class CarouselSkin<T> implements Skin<ListView<?>> {

  /**
   * The vertical alignment of the entire carousel inside the available area, expressed
   * as a fraction of its height.  An alignment of 0.5 centers carousel vertically.
   *
   * Defaults to 0.5.
   *
   * @return the vertical alignment property
   */
  public final DoubleProperty verticalAlignmentProperty() { return verticalAlignment; }
  public final double getVerticalAlignment() { return verticalAlignment.get(); }
  private final DoubleProperty verticalAlignment = new SimpleDoubleProperty(0.5);

  private final ObjectProperty<Layout> layout = new SimpleObjectProperty<>();
  public ObjectProperty<Layout> layoutProperty() { return layout; }
  public Layout getLayout() { return layout.get(); }
  public void setLayout(Layout layout) { this.layout.set(layout); }

  private final Transition transition = new Transition() {
    {
      setCycleDuration(Duration.millis(500));
      setInterpolator(Interpolator.LINEAR);  // frequently restarted animations work very poorly with non-linear Interpolators
    }

    @Override
    protected void interpolate(double frac) {
      absoluteFractionalIndex = startFractionalIndex - startFractionalIndex * frac;
      skin.requestLayout();
    }
  };

  private double startFractionalIndex;
  private double absoluteFractionalIndex;  // The absolute index of the cell which currently is in the carousel center position

  private ListView<?> listView;
  private RenderRegion skin;

  private final CellPool<CarouselListCell<?>> cellPool = new CellPool<>() {
    private Map<Integer, CarouselListCell<?>> cells = new HashMap<>();
    private Map<Integer, CarouselListCell<?>> activeCells = new HashMap<>();
    private List<CarouselListCell<?>> hiddenCells = new ArrayList<>();

    @Override
    public CarouselListCell<?> getCell(int index) {
      CarouselListCell<?> cell = cells.remove(index);

      if(cell != null) {
        activeCells.put(index, cell);

        return cell;
      }

      if(hiddenCells.isEmpty()) {
        cell = createCell();
        skin.getChildren().add(cell);
      }
      else {
        cell = hiddenCells.remove(hiddenCells.size() - 1);
        cell.setVisible(true);
      }

      cell.zoomProperty().addListener(invalidationListener);
      cell.updateIndex(index);

      activeCells.put(index, cell);

      return cell;
    }

    @Override
    public void reset() {
      Map<Integer, CarouselListCell<?>> swap = cells;
      cells = activeCells;
      activeCells = swap;
    }

    @Override
    public void trim() {
      hiddenCells.addAll(cells.values());

      cells.values().forEach(c -> {
        c.setVisible(false);
        c.zoomProperty().removeListener(invalidationListener);
      });
      cells.clear();
    }
  };

  public CarouselListCell<?> createCell() {
    try {
      @SuppressWarnings("unchecked")
      ListView<Object> listView = (ListView<Object>)getSkinnable();
      CarouselListCell<Object> cell = getSkinnable().getCellFactory() == null ? new CarouselListCell<>() :
          (CarouselListCell<Object>)(listView.getCellFactory().call(listView));

      cell.updateListView(listView);

      return cell;
    }
    catch(ClassCastException e) {
      throw new IllegalStateException("Custom CellFactory must return cells which extend CarouselListCell", e);
    }
  }

  private final InvalidationListener invalidationListener = new InvalidationListener() {
    @Override
    public void invalidated(Observable observable) {
      skin.requestLayout();
    }
  };

  public CarouselSkin(final ListView<T> listView) {
    this.listView = listView;

    listView.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
    listView.getStyleClass().add("carousel");
    listView.getSelectionModel().selectedItemProperty().addListener(obs -> skin.requestLayout());   // Calls layout when focused cell changes (to make sure it is at the top)
    listView.widthProperty().addListener(invalidationListener);

    layoutProperty().addListener(invalidationListener);
    verticalAlignmentProperty().addListener(invalidationListener);

    layoutProperty().addListener((obs, old, current) -> {
      if(old != null) {
        old.removeListener(invalidationListener);
      }
      if(current != null) {
        current.addListener(invalidationListener);
      }
    });

    this.skin = new RenderRegion();

    layout.set(new RayLayout());

    listView.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observableValue, Number old, Number current) {

        /*
         * Calculate at how many (fractional) items distance from the middle the carousel currently is and start the transition that will
         * move the now focused cell to the middle.
         */

        startFractionalIndex = absoluteFractionalIndex - old.doubleValue() + current.doubleValue();
        transition.playFromStart();
      }
    });
  }

  @Override
  public ListView<?> getSkinnable() {
    return listView;
  }

  @Override
  public Node getNode() {
    return skin;
  }

  @Override
  public void dispose() {
    this.listView = null;
    this.skin = null;
  }

  private void handleKeyEvent(KeyEvent e) {
    int selectedIndex = getSkinnable().getSelectionModel().getSelectedIndex();
    int oldSelectedIndex = selectedIndex;

    if(e.getCode().isNavigationKey()) {
      if(getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
        if(KeyCode.LEFT == e.getCode()) {
          selectedIndex--;
        }
        else if(KeyCode.RIGHT == e.getCode()) {
          selectedIndex++;
        }
      }
      else {
        if(KeyCode.UP == e.getCode()) {
          selectedIndex--;
        }
        else if(KeyCode.DOWN == e.getCode()) {
          selectedIndex++;
        }
      }

      if(selectedIndex < 0) {
        selectedIndex = 0;
      }
      else if(selectedIndex >= getSkinnable().getItems().size()) {
        selectedIndex = getSkinnable().getItems().size() - 1;
      }

      if(oldSelectedIndex != selectedIndex) {
        getSkinnable().getSelectionModel().select(selectedIndex);
        e.consume();
      }
    }
  }

  public class RenderRegion extends Region {
    @Override
    protected void layoutChildren() {
      Insets insets = getSkinnable().getInsets();

      double x = insets.getLeft();
      double y = insets.getTop();
      double w = getSkinnable().getWidth() - insets.getLeft() - insets.getRight();
      double h = getSkinnable().getHeight() - insets.getTop() - insets.getBottom();

      cellPool.reset();

      Shape cumulativeClip = getLayout().getClipReflections() ? new Rectangle(x - w / 2, y - h * getVerticalAlignment(), w, h) : null;

      /*
       * Cells are returned in front-to-back order.  This is done in order to clip the reflections
       * of cells positioned behind other cells using a cumulative clip.  Reflections would otherwise
       * blend with each other as they are partially transparent.
       */

      int focusedIndex = getSkinnable().getFocusModel().getFocusedIndex();

      CellIterator iterator = getLayout().renderCellIterator(
        focusedIndex - absoluteFractionalIndex,
        getSkinnable().getOrientation(),
        new Dimension2D(getSkinnable().getWidth(), getSkinnable().getHeight()),
        getSkinnable().getItems().size()
      );

      for(int index; (index = iterator.next()) >= 0;) {
        CarouselListCell<?> cell = cellPool.getCell(index);
        Dimension2D size = getCellSize(cell);

        Tuple2<PerspectiveTransform, Shape> tuple = iterator.calculate(index, size, cell.zoomProperty().get(), cell.additionalEffectProperty().get());

        layoutInArea(cell, w / 2, h * getVerticalAlignment(), size.getWidth(), size.getHeight(), 0, HPos.CENTER, VPos.CENTER);

        cell.setEffect(tuple.a);
        cell.setClip(cumulativeClip);  // Clip is set regardless, as it needs to be reset if clipping of reflections is toggled

        getLayout().postProcessCell(iterator, cell, focusedIndex - absoluteFractionalIndex);

        if(getLayout().getClipReflections()) {
          Shape clip = tuple.b;

          if(clip != null) {
            cumulativeClip = Shape.subtract(cumulativeClip, clip);
          }
          else if(cumulativeClip != null) {
            cumulativeClip = Shape.union(cumulativeClip, cumulativeClip);  // Makes a copy as the same clip cannot be part of a scenegraph twice
          }
        }
      }

      cellPool.trim();

      /*
       * Set view orders:
       */

      int selectedIndex = getSkinnable().getSelectionModel().getSelectedIndex();
      int currentIndex = selectedIndex - (int)Math.round(absoluteFractionalIndex);

      for(Node node : getChildren()) {
        node.setViewOrder(Math.abs(((ListCell<?>)node).getIndex() - currentIndex));
      }
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
    public ObservableList<Node> getChildren() {
      return super.getChildren();
    }
  }

  private static Dimension2D getCellSize(Node node) {
    double prefWidth;
    double prefHeight;

    if(node.getContentBias() == Orientation.HORIZONTAL) {
      prefWidth = node.prefWidth(-1);
      prefHeight = node.prefHeight(prefWidth);
    }
    else if(node.getContentBias() == Orientation.VERTICAL) {
      prefHeight = node.prefHeight(-1);
      prefWidth = node.prefWidth(prefHeight);
    }
    else {
      prefWidth = node.prefWidth(-1);
      prefHeight = node.prefHeight(-1);
    }

    return new Dimension2D(prefWidth, prefHeight);
  }
}
