package hs.mediasystem.runner.util;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.PresentationActionEvent;
import hs.mediasystem.runner.ActionTarget;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.util.expose.Trigger;
import hs.mediasystem.util.javafx.SceneUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javafx.animation.Transition;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class DialogPane<R> extends StackPane {
  private final double delay;
  private final Predicate<Event> closeHandler;

  private DialogGlass dialogGlass;
  private boolean synchronous;
  private R result;
  private boolean finished;

  public DialogPane(String styleClass, double delay, Predicate<Event> closeHandler) {
    this.delay = delay;
    this.closeHandler = closeHandler;

    getStyleClass().add(styleClass);

    setMaxWidth(Region.USE_PREF_SIZE);
    setMaxHeight(Region.USE_PREF_SIZE);
  }

  public DialogPane(String styleClass) {
    this(styleClass, 0, e -> true);
  }

  public DialogPane() {
    this("dialog");
  }

  @SuppressWarnings("unchecked")
  public synchronized R showDialog(Scene scene, boolean synchronous) {
    if(finished) {
      return result;
    }

    if(scene == null) {
      throw new IllegalStateException("scene cannot be null");
    }

    this.synchronous = synchronous;

    StackPane.setMargin(this, new Insets(40, 40, 40, 40));

    dialogGlass = new DialogGlass(scene, this, delay);

    NavigablePresentation navigablePresentation = new NavigablePresentation(this);

    addEventHandler(PresentationActionEvent.PROPOSED, e -> {
      ActionTarget actionTarget = e.getAction().getActionTarget();

      if(actionTarget.getActionClass().isAssignableFrom(navigablePresentation.getClass())) {
        Trigger<Object> trigger = actionTarget.createTrigger(e.getAction().getDescriptor(), navigablePresentation);

        if(trigger != null) {
          trigger.run(e, task -> Dialogs.showProgressDialog(e, task));
        }
      }
    });

    requestFocus();

    return synchronous ? (R)SceneUtil.enterNestedEventLoop(this) : null;
  }

  public synchronized void close(R result) {
    if(!finished) {
      this.finished = true;

      if(dialogGlass != null) {
        dialogGlass.remove();
        dialogGlass = null;
      }

      if(synchronous) {
        SceneUtil.exitNestedEventLoop(this, result);
      }
      else {
        this.result = result;
      }
    }
  }

  public synchronized void close() {
    close(null);
  }

  @Override
  public void requestFocus() {
    Node initialFocusNode = lookup(".initial-focus");

    if(initialFocusNode != null) {
      initialFocusNode.requestFocus();
    }
  }

  private static class DialogGlass extends StackPane {
    private final Transition fadeOutAndShowDialog;
    private final StackPane root;
    private final List<Node> children;

    private Node oldFocusOwner;

    public DialogGlass(Scene scene, DialogPane<?> dialogPane, double delay) {
      Parent parent = scene.getRoot();

      if(!(parent instanceof StackPane)) {
        // Reasoning behind having a StackPane as root, is that changing the Scene root will trigger Scene property changes for the entire scene graph, which can have undesirable side effects when used to unregister listeners...
        throw new IllegalStateException("Root of Scene must be a StackPane in order to support Dialogs");
      }

      this.root = (StackPane)parent;
      this.children = new ArrayList<>(root.getChildren());

      for(Node child : children) {
        child.getStyleClass().add("enabled-look");
        child.setDisable(true);
      }

      root.getChildren().add(this);

      oldFocusOwner = scene.getFocusOwner();

      fadeOutAndShowDialog = new Transition() {
        {
          setCycleDuration(Duration.millis(500));
          setDelay(Duration.millis(delay));
        }

        @Override
        protected void interpolate(double frac) {
          dialogPane.setOpacity(frac);
          setBackground(new Background(new BackgroundFill(new Color(0, 0, 0, frac / 2), CornerRadii.EMPTY, Insets.EMPTY)));

          children.stream().forEach(c -> c.setEffect(new GaussianBlur(frac * 5)));
        }
      };

      setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
      setDialog(dialogPane);
    }

    private void setDialog(DialogPane<?> dialogPane) {
      getChildren().add(dialogPane);

      dialogPane.setOpacity(0);

      fadeOutAndShowDialog.play();
    }

    public void remove() {
      for(Node child : root.getChildren()) {
        child.getStyleClass().remove("enabled-look");
        child.setDisable(false);
      }

      if(oldFocusOwner != null) {
        oldFocusOwner.requestFocus();
        oldFocusOwner = null;
      }

      fadeOutAndShowDialog.stop();

      Transition fadeInAndRemove = new Transition() {
        private final double opacityRange = getChildren().get(0).getOpacity();

        {
          setCycleDuration(Duration.millis(500));
        }

        @Override
        protected void interpolate(double frac) {
          double f = (1 - frac) * opacityRange;

          getChildren().get(0).setOpacity(f);
          setBackground(new Background(new BackgroundFill(new Color(0, 0, 0, f / 2), CornerRadii.EMPTY, Insets.EMPTY)));

          children.stream().forEach(c -> c.setEffect(new GaussianBlur(f * 5)));
        }
      };

      fadeInAndRemove.setOnFinished(event -> root.getChildren().remove(DialogGlass.this));
      fadeInAndRemove.play();
    }
  }

  public static class NavigablePresentation implements Presentation, Navigable {
    private final DialogPane<?> dialogPane;

    public NavigablePresentation(DialogPane<?> dialogPane) {
      this.dialogPane = dialogPane;
    }

    @Override
    public void navigateBack(Event e) {
      if(dialogPane.closeHandler.test(e)) {
        dialogPane.close();
      }
    }
  }
}
