package hs.mediasystem.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.SimpleObjectProperty;

public class NavigableProperty<T> extends SimpleObjectProperty<T> {

  /**
   * Contains all previous values plus the current value.
   */
  private final List<T> navigationStack = new ArrayList<>();

  private int position;

  {
    navigationStack.add(null);
  }

  @Override
  public void set(T newValue) {
    T oldValue = get();

    super.set(newValue);

    if(!Objects.equals(oldValue, newValue)) {
      while(position < navigationStack.size() - 1) {
        navigationStack.remove(navigationStack.size() - 1);
      }

      navigationStack.add(newValue);
      position++;
    }
  }

  public void update(T value) {

  }

  public List<T> getHistory() {
    return Collections.unmodifiableList(navigationStack.subList(0, position + 1));
  }

  public void setHistory(List<T> values) {
    navigationStack.clear();
    navigationStack.addAll(values);

    position = navigationStack.size() - 1;

    super.set(navigationStack.get(position));  // Donot call set, as we don't want to store this navigation action
  }

  public boolean back() {
    if(position == 0) {
      return false;
    }

    super.set(navigationStack.get(--position));  // Donot call set, as we don't want to store this navigation action

    return true;
  }

  public boolean forward() {
    if(position == navigationStack.size() - 1) {
      return false;
    }

    super.set(navigationStack.get(++position));

    return true;
  }
}
