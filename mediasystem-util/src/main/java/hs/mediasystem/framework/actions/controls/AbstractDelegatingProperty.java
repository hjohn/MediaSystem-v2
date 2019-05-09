package hs.mediasystem.framework.actions.controls;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public abstract class AbstractDelegatingProperty<T, P extends Property<T>> implements Property<T> {
  private final P delegate;

  public AbstractDelegatingProperty(P delegate) {
    this.delegate = delegate;
  }

  protected P getDelegate() {
    return delegate;
  }

  @Override
  public T getValue() {
    return delegate.getValue();
  }

  @Override
  public void setValue(T value) {
    delegate.setValue(value);
  }

  @Override
  public Object getBean() {
    return delegate.getBean();
  }

  @Override
  public void bind(ObservableValue<? extends T> observable) {
    delegate.bind(observable);
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public void unbind() {
    delegate.unbind();
  }

  @Override
  public void addListener(InvalidationListener listener) {
    delegate.addListener(listener);
  }

  @Override
  public boolean isBound() {
    return delegate.isBound();
  }

  @Override
  public void bindBidirectional(Property<T> other) {
    delegate.bindBidirectional(other);
  }

  @Override
  public void removeListener(InvalidationListener listener) {
    delegate.removeListener(listener);
  }

  @Override
  public void unbindBidirectional(Property<T> other) {
    delegate.unbindBidirectional(other);
  }

  @Override
  public void addListener(ChangeListener<? super T> listener) {
    delegate.addListener(listener);
  }

  @Override
  public void removeListener(ChangeListener<? super T> listener) {
    delegate.removeListener(listener);
  }
}
