package hs.mediasystem.util.javafx.action;

import javafx.beans.binding.StringExpression;
import javafx.event.Event;

import org.reactfx.value.Val;

public interface Action {
  StringExpression titleProperty();
  Val<Boolean> enabledProperty();
  void trigger(Event event);
}
