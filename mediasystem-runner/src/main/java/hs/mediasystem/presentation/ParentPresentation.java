package hs.mediasystem.presentation;

import hs.mediasystem.runner.util.debug.DebugFX;
import hs.mediasystem.util.javafx.base.Events;

import java.util.ArrayDeque;
import java.util.Deque;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;

public class ParentPresentation implements Presentation, Navigable {
  public final ObjectProperty<Presentation> childPresentation = new SimpleObjectProperty<>();

  private final Deque<Presentation> previousPresentations = new ArrayDeque<>();
  private final ChangeListener<? super Presentation> listener = (obs, old, current) -> {
    if(old != null) {
      previousPresentations.add(old);
    }
  };

  {
    childPresentation.addListener(listener);

    DebugFX.addReference(this);
  }

  @Override
  public void navigateBack(Event e) {
    if(previousPresentations.isEmpty()) {
      return;
    }

    Presentation presentation = previousPresentations.removeLast();

    childPresentation.removeListener(listener);
    childPresentation.set(presentation);
    childPresentation.addListener(listener);

    /*
     * Note: the refresh event should be sent to the newly focused
     * node; the target of the navigate event would point to the UI about to
     * disappear.
     */

    Events.dispatchEvent(e.getTarget(), PresentationEvent.requestFocusedRefresh());

    e.consume();
  }
}
