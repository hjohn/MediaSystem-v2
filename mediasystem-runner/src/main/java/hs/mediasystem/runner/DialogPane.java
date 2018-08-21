package hs.mediasystem.runner;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.util.Exceptional;

import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class DialogPane<R> extends StackPane {
  private final double delay;

  private DialogGlass dialogGlass;
  private boolean synchronous;
  private Throwable exceptionalResult;
  private R result;
  private boolean finished;

  public DialogPane(double delay) {
    this.delay = delay;

    getStyleClass().add("dialog");

    setMaxWidth(Region.USE_PREF_SIZE);
    setMaxHeight(Region.USE_PREF_SIZE);
  }

  public DialogPane() {
    this(0);
  }

  @SuppressWarnings("unchecked")
  public synchronized Exceptional<R> showDialog(Scene scene, boolean synchronous) {
    if(finished) {
      return exceptionalResult != null ? Exceptional.ofException(exceptionalResult) : Exceptional.ofNullable(result);
    }

    if(scene == null) {
      throw new IllegalStateException("scene cannot be null");
    }

    this.synchronous = synchronous;

    StackPane.setMargin(this, new Insets(40, 40, 40, 40));

    dialogGlass = new DialogGlass(scene, this, delay);
    scene.getRoot().getProperties().put("presentation2", new NavigablePresentation(this));

    requestFocus();

    if(synchronous) {
      return (Exceptional<R>)Platform.enterNestedEventLoop(this);
    }

    return null;
  }

  public synchronized void closeExceptionally(Throwable result) {
    if(!finished) {
      this.finished = true;

      if(dialogGlass != null) {
        dialogGlass.remove();
        dialogGlass = null;
      }

      if(synchronous) {
        Platform.exitNestedEventLoop(this, Exceptional.ofException(result));
      }
      else {
        this.exceptionalResult = result;
      }
    }
  }

  public synchronized void close(R result) {
    if(!finished) {
      this.finished = true;

      if(dialogGlass != null) {
        dialogGlass.remove();
        dialogGlass = null;
      }

      if(synchronous) {
        Platform.exitNestedEventLoop(this, Exceptional.ofNullable(result));
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

    private Node oldFocusOwner;

    public DialogGlass(Scene scene, DialogPane<?> dialogPane, double delay) {
      Parent parent = scene.getRoot();

      if(!(parent instanceof StackPane)) {
        // Reasoning behind having a StackPane as root, is that changing the Scene root will trigger Scene property changes for the entire scene graph, which can have undesirable side effects when used to unregister listeners...
        throw new IllegalStateException("Root of Scene must be a StackPane in order to support Dialogs");
      }

      this.root = (StackPane)parent;

      for(Node child : root.getChildren()) {
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
      root.getProperties().remove("presentation2");  // TODO donot like this... quick dialogs in succession will remove each other's presentation

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
        }
      };

      fadeInAndRemove.setOnFinished(event -> {
        root.getChildren().remove(DialogGlass.this);
//        int size = root.getChildren().size();
//
//        if(root.getChildren().get(size - 1).equals(DialogGlass.this)) {
//          root.getChildren().remove(size - 1);
//        }
//        else {
//          System.out.println("+++++++++++++++++++ REMVOED NOTHING  +++++++++++++++++++");
//        }
      });

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
      dialogPane.close();
    }
  }
}
