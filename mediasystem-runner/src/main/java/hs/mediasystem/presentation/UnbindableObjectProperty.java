package hs.mediasystem.presentation;

import hs.mediasystem.util.javafx.property.Unbinder;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class UnbindableObjectProperty<T> extends SimpleObjectProperty<T> {
  private final Unbinder unbinder;

  public UnbindableObjectProperty(Unbinder unbinder) {
    this.unbinder = unbinder;
  }

  @Override
  public void addListener(ChangeListener<? super T> listener) {
    super.addListener(listener);

    unbinder.add(() -> removeListener(listener));
  }

  @Override
  public void addListener(InvalidationListener listener) {
    super.addListener(listener);

    unbinder.add(() -> removeListener(listener));
  }

  @Override
  public void bind(ObservableValue<? extends T> newObservable) {
    super.bind(newObservable);

    unbinder.add(() -> unbind());
  }
}
