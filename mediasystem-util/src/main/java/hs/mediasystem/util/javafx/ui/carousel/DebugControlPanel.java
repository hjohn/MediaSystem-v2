package hs.mediasystem.util.javafx.ui.carousel;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DebugControlPanel extends VBox {
  private final Layout layout;
  private final GridPane optionGridPane = new GridPane();

  public DebugControlPanel(Layout layout) {
    this.layout = layout;

    setPadding(new Insets(20.0));

    getChildren().addAll(optionGridPane);

    fillOptionGridPane(layout);
  }

  public void unbind(Layout genericLayout) {
    if(genericLayout instanceof RayLayout) {
      RayLayout layout = (RayLayout)genericLayout;

      layout.radiusRatioProperty().unbind();
      layout.viewDistanceRatioProperty().unbind();
      layout.carouselViewFractionProperty().unbind();
      layout.viewAlignmentProperty().unbind();
    }
  }

  public void fillOptionGridPane(Layout genericLayout) {
    row = 0;
    optionGridPane.getChildren().clear();

    addSlider(layout.cellAlignmentProperty(), "%4.2f", "Cell Alignment (0.0 - 1.0)", 0.0, 1.0, 0.1, "The vertical alignment of cells which donot utilize all of the maximum available height");
    addSlider(layout.densityProperty(), "%6.4f", "Cell Density (0.001 - 0.1)", 0.001, 0.1, 0.0025, "The density of cells in cells per pixel of view width");
    addSlider(layout.maxCellWidthProperty(), "%4.0f", "Maximum Cell Width (1 - 2000)", 1, 1000, 5, "The maximum width a cell is allowed to become");
    addSlider(layout.maxCellHeightProperty(), "%4.0f", "Maximum Cell Height (1 - 2000)", 1, 1000, 5, "The maximum height a cell is allowed to become");

    optionGridPane.add(new HBox() {{
      setSpacing(20);
      getChildren().add(new CheckBox("Reflections?") {{
        setStyle("-fx-font-size: 16px");
        selectedProperty().bindBidirectional(layout.reflectionEnabledProperty());
      }});
      getChildren().add(new CheckBox("Clip Reflections?") {{
        setStyle("-fx-font-size: 16px");
        selectedProperty().bindBidirectional(layout.clipReflectionsProperty());
      }});
    }}, 2, row++);

    if(genericLayout instanceof RayLayout) {
      RayLayout layout = (RayLayout)genericLayout;

      addSlider(layout.centerPositionProperty(), "%4.2f", "Center Position (0.0 - 1.0)", 0.0, 1.0, 0.1, "The horizontal position of the carousel center expressed as the fraction of thalf the view width");
      addSlider(layout.radiusRatioProperty(), "%4.2f", "Radius Ratio (0.0 - 2.0)", 0.0, 2.0, 0.1, "The radius of the carousel expressed as the fraction of half the view width");
      addSlider(layout.viewDistanceRatioProperty(), "%4.2f", "View Distance Ratio (0.0 - 4.0)", 0.0, 4.0, 0.1, "The distance of the camera expressed as a fraction of the radius of the carousel");
      addSlider(layout.carouselViewFractionProperty(), "%4.2f", "Carousel View Fraction (0.0 - 1.0)", 0.0, 1.0, 0.1, "The portion of the carousel that is used for displaying cells");
      addSlider(layout.viewAlignmentProperty(), "%4.2f", "View Alignment (0.0 - 1.0)", 0.0, 1.0, 0.1, "The vertical alignment of the camera with respect to the carousel");
    }
  }

  private int row = 1;

  private void addSlider(final DoubleProperty property, final String format, String description, double min, double max, final double increment, String longDescription) {
    optionGridPane.add(new Label(description) {{
      setStyle("-fx-font-size: 16px");
    }}, 1, row);
    optionGridPane.add(new Slider(min, max, property.get()) {{
      setStyle("-fx-font-size: 16px");
      valueProperty().bindBidirectional(property);
      setBlockIncrement(increment);
      setMinWidth(400);
    }}, 2, row);
    optionGridPane.add(new Label() {{
      setStyle("-fx-font-size: 16px");
      textProperty().bind(property.asString(format));
    }}, 3, row);

    row++;

    optionGridPane.add(new Label(longDescription) {{
      setPadding(new Insets(0, 0, 5, 0));
    }}, 1, row, 3, 1);

    row++;
  }
}
