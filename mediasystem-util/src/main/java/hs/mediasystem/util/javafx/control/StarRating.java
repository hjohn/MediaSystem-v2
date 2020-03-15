package hs.mediasystem.util.javafx.control;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public class StarRating extends StackPane {
  private final DoubleProperty rating = new SimpleDoubleProperty(0.0);
  public DoubleProperty ratingProperty() { return rating; }
  public double getRating() { return rating.get(); }
  public void setRating(double rating) { this.rating.set(rating); }

  private final Rectangle rectangle = new Rectangle(0, 0, 0, 0);

  public StarRating(final double radius, final double innerRadius, final int numberOfPoints) {
    getStyleClass().add("star-rating");

    double[] starData = createStarData(radius, innerRadius, numberOfPoints);

    HBox stars = createStars(starData);
    HBox disabledStars = createStars(starData);

    stars.setMaxWidth(radius * 10);
    disabledStars.setMaxWidth(radius * 10);

    stars.setClip(rectangle);
    disabledStars.getStyleClass().add("background");

    getChildren().addAll(disabledStars, stars);

    rectangle.widthProperty().bind(rating.multiply(disabledStars.widthProperty()));
    rectangle.heightProperty().bind(disabledStars.heightProperty());
    disabledStars.opacityProperty().bind(Bindings.when(rating.isEqualTo(0.0, 0.0)).then(0.0).otherwise(1.0));
  }

  private static HBox createStars(final double[] points) {
    return new HBox() {{
      for(int i = 0; i < 5; i++) {
        getChildren().add(new Polygon(points) {{
          getStyleClass().add("star");
        }});
      }
    }};
  }

  private static double[] createStarData(double radius, double innerRadius, int numberOfPoints) {
    double[] points = new double[numberOfPoints * 4];
    double[] radii = {radius, innerRadius};
    int p = 0;

    for(int i = 0; i < numberOfPoints * 2; i++) {
      points[p++] = radii[i % 2] * -Math.sin(i * Math.PI / numberOfPoints);
      points[p++] = radii[i % 2] * -Math.cos(i * Math.PI / numberOfPoints);
    }

    return points;
  }
}
