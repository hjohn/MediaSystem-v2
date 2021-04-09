package hs.mediasystem.util.javafx.action;

import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.Event;

public interface Action {
  StringExpression titleProperty();
  ReadOnlyBooleanProperty enabledProperty();
  void trigger(Event event);
}
