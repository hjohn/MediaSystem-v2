package hs.mediasystem.util.javafx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;

public class WeakBinder {
  private final List<Object> hardRefs = new ArrayList<>();
  private final Map<ObservableValue<?>, WeakInvalidationListener> listeners = new HashMap<>();

  public void unbindAll() {
    for(ObservableValue<?> observableValue : listeners.keySet()) {
      observableValue.removeListener(listeners.get(observableValue));
    }

    hardRefs.clear();
    listeners.clear();
  }

  public <T> void bind(final Property<T> property, final ObservableValue<? extends T> dest) {
    InvalidationListener invalidationListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        property.setValue(dest.getValue());
      }
    };

    WeakInvalidationListener weakInvalidationListener = new WeakInvalidationListener(invalidationListener);

    listeners.put(dest, weakInvalidationListener);

    dest.addListener(weakInvalidationListener);
    property.setValue(dest.getValue());

    hardRefs.add(invalidationListener);
  }
}
