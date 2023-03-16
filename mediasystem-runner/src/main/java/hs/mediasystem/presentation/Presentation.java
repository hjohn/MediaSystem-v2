package hs.mediasystem.presentation;

import hs.mediasystem.util.javafx.base.EventHandlerTarget;

public interface Presentation {

  /**
   * Fetches the most recent data for this presentation and stores the
   * result in an update task.  Running the resulting task will update
   * the presentation with the refreshed data.  Note that the resulting
   * task must be run on the UI thread if the Presentation is currently
   * attached to a UI.<p>
   *
   * This is a two step operation, where the creation of the update task
   * can be expensive and should be run in the background.  The resulting
   * update task should be fast.
   *
   * @return a {@link Runnable} which updates the presentation when run, never null
   */
  default Runnable createUpdateTask() {
    return () -> {};
  }

  /**
   * Associates this {@link Presentation} to the given event handler target, so that it may
   * process {@link PresentationEvent}s relevant to it
   *
   * @param eventHandlerTarget an {@link EventHandlerTarget}, cannot be {@code null}
   * @throws NullPointerException when {@code eventHandlerTarget} is {@code null}
   */
  default void associate(EventHandlerTarget eventHandlerTarget) {
    eventHandlerTarget.addEventFilter(PresentationEvent.ANY, e -> e.addPresentation(this));
  }
}
