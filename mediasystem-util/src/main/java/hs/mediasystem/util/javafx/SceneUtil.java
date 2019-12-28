package hs.mediasystem.util.javafx;

import hs.mediasystem.util.FocusEvent;
import hs.mediasystem.util.NamedThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

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
          oldValue.getStyleClass().remove("focused");
          oldValue.fireEvent(new FocusEvent(false));
          LOGGER.fine("Focus removed from: " + oldValue);
        }
        if(newValue != null) {
          newValue.getStyleClass().add("focused");
          newValue.fireEvent(new FocusEvent(true));

          LOGGER.fine("Focus set to: " + newValue + " (old=" + oldValue + ")");
        }
      }
    });
  }

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

        Event returnedEvent = eventDispatcher.dispatchEvent(event, tail);

        long duration = System.currentTimeMillis() - startTime;

        if(duration > 10) {
          LOGGER.warning("Slow Event (" + duration + " ms): " + event);
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
