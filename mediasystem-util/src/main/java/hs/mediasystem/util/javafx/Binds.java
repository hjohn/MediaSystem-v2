package hs.mediasystem.util.javafx;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

public class Binds {

  public static <T> Val<T> monadic(ObservableValue<T> observableValue) {
    return new MonadicObservableValueAdapter<>(observableValue);
  }

  public static <T> Val<T> monadic(T value) {
    return new MonadicObservableValueAdapter<>(new SimpleObjectProperty<>(value));
  }
}
