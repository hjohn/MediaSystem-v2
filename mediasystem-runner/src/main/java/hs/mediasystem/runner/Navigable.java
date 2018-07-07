package hs.mediasystem.runner;

import hs.mediasystem.framework.actions.Expose;

import javafx.event.Event;

public interface Navigable {
  @Expose
  void navigateBack(Event e);
}
