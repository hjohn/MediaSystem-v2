package hs.mediasystem.util.javafx;

import hs.mediasystem.util.FocusEvent;
import hs.mediasystem.util.NamedThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class SceneUtil {
  private static final Logger LOGGER = Logger.getLogger(SceneUtil.class.getName());
  private static final ScheduledExecutorService EVENT_TIMEOUT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("SceneUtil"));
  private static final NestedTimeTracker TIME_TRACKER = new NestedTimeTracker();

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

        if(node instanceof Button) {
          Button button = (Button)node;

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
   * Adds a slow event warning whenever an event takes more than 10 ms to process.  Note
   * that time spent in nested event loops cannot be properly taken into account as time
   * spent in nested event loops will be part of the event that triggered it giving false
   * positives.  In order for this time to be accurately reflected, the methods to enter
   * a nested event loop in this class should be used instead of the ones in {@link Platform}.
   *
   * @param scene a Scene to which to add the slow event warning detection, cannot be null
   */
  public static void addSlowEventWarning(Scene scene) {
    final EventDispatcher eventDispatcher = scene.getEventDispatcher();

    scene.setEventDispatcher(new EventDispatcher() {
      private ScheduledFuture<StackTraceElement[]> future;

      @Override
      public Event dispatchEvent(Event event, EventDispatchChain tail) {
        Thread fxThread = Thread.currentThread();

        if(future != null) {
          future.cancel(false);
        }

//        future = EVENT_TIMEOUT_EXECUTOR.schedule(new Callable<StackTraceElement[]>() {
//          @Override
//          public StackTraceElement[] call() {
//            return fxThread.getStackTrace();
//          }
//        }, 500, TimeUnit.MILLISECONDS);

        long startTime = System.currentTimeMillis();

        TIME_TRACKER.enterNested(startTime);  // nesting can happen in two ways, an event triggering another event, or when a nested event loop is entered

        Event returnedEvent = eventDispatcher.dispatchEvent(event, tail);

        long endTime = System.currentTimeMillis();
        long timeSpentInNested = TIME_TRACKER.exitNested(endTime);

        if(timeSpentInNested > 10) {
          long total = endTime - startTime;

          LOGGER.warning("Slow Event (self/total: " + timeSpentInNested + "/" + total + " ms @ level " + TIME_TRACKER.getCurrentLevel() + "): " + event);
        }

//        future.cancel(false);
//
//        if(!future.isCancelled()) {  // separate check, as future may have been cancelled already multiple times, so cannot use result from Future#cancel
//          try {
//            LOGGER.warning("Slow Event Handling (" + duration + " ms) of " + event + ":");
//
//            for(StackTraceElement element : future.get()) {
//              LOGGER.warning("  -- " + element);
//            }
//          }
//          catch(InterruptedException | ExecutionException e) {
//            LOGGER.log(Level.SEVERE, "Unexpected exception", e);
//          }
//        }

        return returnedEvent;
      }
    });
  }
}
