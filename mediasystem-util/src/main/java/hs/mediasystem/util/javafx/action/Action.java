package hs.mediasystem.util.javafx.action;

import hs.mediasystem.util.javafx.Val;

import javafx.beans.binding.StringExpression;
import javafx.event.Event;

public interface Action {
  StringExpression titleProperty();
  Val<Boolean> enabledProperty();
  void trigger(Event event);
}
