package hs.mediasystem.presentation;

public interface Presentation {

  /**
   * Fetches the most recent data for this presentation and stores the
   * result in an update task.  Running the resulting task will update
   * the presentation with the refreshed data.  Note that this task must
   * be run on the UI thread if the Presentation is currently attached to
   * a UI.<p>
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

}
