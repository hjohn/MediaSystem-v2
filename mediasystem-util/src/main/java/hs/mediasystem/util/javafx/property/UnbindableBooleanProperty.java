package hs.mediasystem.util.javafx.property;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

public class UnbindableBooleanProperty extends SimpleBooleanProperty implements Unbindable {

  public UnbindableBooleanProperty(boolean b) {
    super(b);
  }

  public UnbindableBooleanProperty() {
    this(false);
  }

  @Override
  public void addListener(ChangeListener<? super Boolean> listener) {
    super.addListener(listener);

    addBindingRemover(() -> removeListener(listener));
  }

  @Override
  public void addListener(InvalidationListener listener) {
    super.addListener(listener);

    addBindingRemover(() -> removeListener(listener));
  }
}
