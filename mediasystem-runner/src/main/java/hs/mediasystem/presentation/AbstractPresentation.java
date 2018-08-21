package hs.mediasystem.presentation;

import hs.mediasystem.runner.DebugFX;
import hs.mediasystem.util.javafx.property.Unbinder;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;

public abstract class AbstractPresentation implements Presentation {
  protected final Unbinder unbinder = new Unbinder();

  {
    DebugFX.addReference(this);
  }

  public void unbindAll() {
    System.out.println("^^^^^^ UNBIND ALL for " + this);
    unbinder.unbindAll();
  }

  protected <T> UnbindableObjectProperty<T> objectProperty() {
    return new UnbindableObjectProperty<>(unbinder);
  }

  protected UnbindableBooleanProperty booleanProperty(boolean b) {
    return new UnbindableBooleanProperty(unbinder, b);
  }

  protected UnbindableBooleanProperty booleanProperty() {
    return booleanProperty(false);
  }

  protected <T> UnbindableReadOnlyObjectProperty<T> objectValue(ObjectProperty<T> property) {
    return new UnbindableReadOnlyObjectProperty<>(unbinder, property);
  }

  protected UnbindableReadOnlyIntegerProperty integerValue(IntegerProperty property) {
    return new UnbindableReadOnlyIntegerProperty(unbinder, property);
  }
}
