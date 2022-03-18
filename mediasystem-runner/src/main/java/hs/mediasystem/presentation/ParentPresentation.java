package hs.mediasystem.presentation;

import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.util.DebugFX;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Determine presentation stack for refreshing first, then refresh it.
     *
     * The stack should be the presentation to be shown plus any of its
     * child presentations.
     */

    List<Presentation> presentations = Stream.iterate(
        presentation,
        Objects::nonNull,
        p -> p instanceof ParentPresentation pp ? pp.childPresentation.get() : null
      )
      .collect(Collectors.toList());

    Presentations.refreshPresentations(e, presentations);

    e.consume();
  }
}
