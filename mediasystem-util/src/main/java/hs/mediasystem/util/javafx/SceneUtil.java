package hs.mediasystem.util.javafx;

import com.sun.javafx.tk.Toolkit;

import hs.mediasystem.util.FocusEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
  private static final ScheduledExecutorService EVENT_TIMEOUT_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

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
      @Override
      public Event dispatchEvent(Event event, EventDispatchChain tail) {
        Thread fxThread = Thread.currentThread();

        ScheduledFuture<?> future = EVENT_TIMEOUT_EXECUTOR.schedule(new Runnable() {
          @Override
          public void run() {
            if(!Toolkit.getToolkit().isNestedLoopRunning()) {
              LOGGER.warning("Slow Event Handling, trace:");

              for(StackTraceElement element : fxThread.getStackTrace()) {
                LOGGER.warning("  -- " + element);
              }
            }
          }
        }, 100, TimeUnit.MILLISECONDS);

        Event returnedEvent = eventDispatcher.dispatchEvent(event, tail);

        future.cancel(false);

        return returnedEvent;
      }
    });
  }
}
