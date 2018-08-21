package hs.mediasystem.util.javafx;

import javafx.beans.binding.StringExpression;
import javafx.event.Event;

public interface Action {
  StringExpression titleProperty();
  Val<Boolean> enabledProperty();
  void trigger(Event event);
}
