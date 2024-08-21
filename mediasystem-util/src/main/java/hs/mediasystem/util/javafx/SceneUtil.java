package hs.mediasystem.util.javafx;

import hs.mediasystem.util.javafx.base.FocusEvent;

import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventDispatcher;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class SceneUtil {
  private static final Logger LOGGER = Logger.getLogger(SceneUtil.class.getName());
  private static final NestedTimeTracker TIME_TRACKER = new NestedTimeTracker();
  private static final long SLOW_EVENT_THRESHOLD_NANOS = 10 * 1000 * 1000;  // 10 milliseconds

  public static Scene createScene(Parent root) {
    Scene scene = new Scene(root);

    addSlowEventWarning(scene);
    addFocusedStyle(scene);
    buttonsRespondToEnter(scene);

    return scene;
  }

  private static void buttonsRespondToEnter(Scene scene) {
    scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
      if(!e.isConsumed() && e.getCode() == KeyCode.ENTER) {
        Node node = scene.getFocusOwner();

        if(node instanceof Button button) {
          button.fire();
          e.consume();
        }
      }
    });
  }

  private static void addFocusedStyle(Scene scene) {
    scene.focusOwnerProperty().addListener(new ChangeListener<Node>() {  // WORKAROUND for lack of Focus information when Stage is not focused
      @Override
      public void changed(ObservableValue<? extends Node> observable, Node oldValue, Node newValue) {
        if(oldValue != null) {
          // Made async because some components (markdown view) donot like having their styles changed right away it seems
          Platform.runLater(() -> {
            oldValue.getStyleClass().remove("focused");
            oldValue.fireEvent(new FocusEvent(false));
          });

          LOGGER.fine("Focus removed from: " + oldValue);
        }
        if(newValue != null) {
          Platform.runLater(() -> {
            newValue.getStyleClass().add("focused");
            newValue.fireEvent(new FocusEvent(true));
          });

          LOGGER.fine("Focus set to: " + newValue + " (old=" + oldValue + ")");
        }
      }
    });
  }

  public static Object enterNestedEventLoop(Object key) {
    TIME_TRACKER.enterNested();

    return Platform.enterNestedEventLoop(key);
  }

  public static void exitNestedEventLoop(Object key, Object rval) {
    Platform.exitNestedEventLoop(key, rval);

    TIME_TRACKER.exitNested();
  }

  /**
   * Adds a slow event warning whenever an event takes too much time to process. Note
   * that time spent in nested event loops cannot be properly taken into account as time
   * spent in nested event loops will be part of the event that triggered it giving false
   * positives. In order for this time to be accurately reflected, the methods to enter
   * a nested event loop in this class should be used instead of the ones in {@link Platform}.
   *
   * @param scene a {@link Scene} to add the slow event warning detection to, cannot be {@code null}
   */
  public static void addSlowEventWarning(Scene scene) {
    EventDispatcher eventDispatcher = scene.getEventDispatcher();

    scene.setEventDispatcher((event, tail) -> {
      long startTime = System.nanoTime();

      TIME_TRACKER.enterNested(startTime);  // nesting can happen in two ways, an event triggering another event, or when a nested event loop is entered

      Event returnedEvent = eventDispatcher.dispatchEvent(event, tail);

      long endTime = System.nanoTime();
      long timeSpentAtThisLevel = TIME_TRACKER.exitNested(endTime);
      long dispatchTime = endTime - startTime;

      /*
       * There are two duration measurements that are of importance:
       *
       * - The dispatch duration of the event (duration of the dispatchEvent call)
       * - The time spent at the current nesting level, minus the time spent at any deeper levels
       *
       * If the dispatch time is below the threshold (regardless of nesting levels
       * entered or exited) then the event is considered to be fast.
       *
       * However, if the dispatch time exceeds the threshold then the time spent
       * at only the current nesting level needs to be used instead. Note that for
       * events exiting a level, this can be a high value, which is why the dispatch
       * time must be checked first to avoid false positives.
       *
       * For example, an event E1 comes in at T1 which opens a dialog that uses a nested
       * event loop. The dialog stays open for 5 seconds. Another event, E2, comes in
       * to close the dialog exiting the nested event loop.
       *
       * E1 took a long time to dispatch before it returned (5s). When subtracting
       * the nested time, E1 only took a few ms.
       *
       * E2 took almost no time to dispatch. The time spent in the nested loop is disregarded.
       */

      if(dispatchTime > SLOW_EVENT_THRESHOLD_NANOS && timeSpentAtThisLevel > SLOW_EVENT_THRESHOLD_NANOS) {
        LOGGER.warning("Slow Event (self/total: " + (timeSpentAtThisLevel / 1000 / 1000) + "/" + (dispatchTime / 1000 / 1000) + " ms @ level " + TIME_TRACKER.getCurrentLevel() + "): " + event.toString().replaceAll("[\t\r\n]", ""));
      }

      return returnedEvent;
    });
  }
}
