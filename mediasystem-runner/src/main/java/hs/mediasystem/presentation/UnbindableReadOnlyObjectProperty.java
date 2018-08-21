package hs.mediasystem.presentation;

import hs.mediasystem.util.javafx.Unbinder;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.value.ChangeListener;

public class UnbindableReadOnlyObjectProperty<T> extends ReadOnlyObjectPropertyBase<T> {
  private final Unbinder unbinder;
  private final ObjectProperty<T> delegate;

  public UnbindableReadOnlyObjectProperty(Unbinder unbinder, ObjectProperty<T> delegate) {
    this.unbinder = unbinder;
    this.delegate = delegate;
    this.delegate.addListener(obs -> fireValueChangedEvent());
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
  public T get() {
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
