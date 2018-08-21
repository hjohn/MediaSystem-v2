package hs.mediasystem.presentation;

import hs.mediasystem.util.javafx.Unbinder;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

public class UnbindableBooleanProperty extends SimpleBooleanProperty {
  private final Unbinder unbinder;

  public UnbindableBooleanProperty(Unbinder unbinder, boolean b) {
    super(b);

    this.unbinder = unbinder;
  }

  @Override
  public void addListener(ChangeListener<? super Boolean> listener) {
    super.addListener(listener);

    unbinder.add(() -> removeListener(listener));
  }

  @Override
  public void addListener(InvalidationListener listener) {
    super.addListener(listener);

    unbinder.add(() -> removeListener(listener));
  }
}
