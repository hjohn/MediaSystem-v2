package hs.mediasystem.util.javafx;

import javafx.beans.value.ObservableValue;

public class Binds {

  public static <T> MonadicObservableValue<T> monadic(ObservableValue<T> observableValue) {
    return new MonadicObservableValueAdapter<>(observableValue);
  }
}
