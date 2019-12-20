package hs.mediasystem.util.javafx.control.carousel;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ListCell;
import javafx.scene.effect.Effect;

public class CarouselListCell<T> extends ListCell<T> {
  private final DoubleProperty zoomProperty = new SimpleDoubleProperty(1.0);
  public DoubleProperty zoomProperty() { return zoomProperty; }

  private final ObjectProperty<Effect> additionalEffectProperty = new SimpleObjectProperty<>();
  public ObjectProperty<Effect> additionalEffectProperty() { return additionalEffectProperty; }
}
