package hs.mediasystem.util.javafx;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class MonadicObservableValueAdapter<T> implements MonadicObservableValue<T> {
  private final ObservableValue<T> observableValue;

  public MonadicObservableValueAdapter(ObservableValue<T> observableValue) {
    this.observableValue = observableValue;
  }

  @Override
  public void addListener(ChangeListener<? super T> listener) {
    observableValue.addListener(listener);
  }

  @Override
  public void removeListener(ChangeListener<? super T> listener) {
    observableValue.removeListener(listener);
  }

  @Override
  public T getValue() {
    return observableValue.getValue();
  }

  @Override
  public void addListener(InvalidationListener listener) {
    observableValue.addListener(listener);
  }

  @Override
  public void removeListener(InvalidationListener listener) {
    observableValue.removeListener(listener);
  }

}
