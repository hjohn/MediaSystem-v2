package hs.mediasystem.util.javafx.property;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

public class UnbindableObjectProperty<T> extends SimpleObjectProperty<T> implements Unbindable {
  private final Unbinder unbinder;

  public UnbindableObjectProperty(Unbinder unbinder, T initialValue) {
    super(initialValue);

    this.unbinder = unbinder;
  }

  public UnbindableObjectProperty(Unbinder unbinder) {
    this(unbinder, null);
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
}
