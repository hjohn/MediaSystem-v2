package hs.mediasystem.presentation;

import hs.mediasystem.util.javafx.property.Unbinder;

import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerPropertyBase;
import javafx.beans.value.ChangeListener;

public class UnbindableReadOnlyIntegerProperty extends ReadOnlyIntegerPropertyBase {
  private final Unbinder unbinder;
  private final IntegerProperty delegate;

  public UnbindableReadOnlyIntegerProperty(Unbinder unbinder, IntegerProperty delegate) {
    this.unbinder = unbinder;
    this.delegate = delegate;
    this.delegate.addListener(obs -> fireValueChangedEvent());
  }

  @Override
  public void addListener(ChangeListener<? super Number> listener) {
    super.addListener(listener);

    unbinder.add(() -> removeListener(listener));
  }

  @Override
  public void addListener(InvalidationListener listener) {
    super.addListener(listener);

    unbinder.add(() -> removeListener(listener));
  }

  @Override
  public int get() {
    return delegate.get();
  }

  @Override
  public Object getBean() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }
}
