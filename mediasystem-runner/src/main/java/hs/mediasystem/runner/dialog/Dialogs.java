package hs.mediasystem.runner.dialog;

import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.MarkdownTextView;
import hs.mediasystem.util.Localizable;
import hs.mediasystem.util.exception.Throwables;
import hs.mediasystem.util.javafx.control.Buttons;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class Dialogs {
  private static final String STYLES_URL = LessLoader.compile(Dialogs.class, "dialogs.less");

  public static void show(Event event, String dialogStyleClass, Node content) {
    DialogPane<Void> dialogPane = new DialogPane<>(dialogStyleClass);

    dialogPane.getChildren().add(content);

    dialogPane.showDialog(((Node)event.getTarget()).getScene(), true);
  }

  public static void show(Event event, Node content) {
    show(event, "dialog", content);
  }

  public static <T> Optional<T> show(Event event, Node content, Map<T, String> options) {
    DialogPane<T> dialogPane = new DialogPane<>();
    VBox vbox = new VBox();

    if(content != null) {
      vbox.getChildren().add(content);
    }

    for(Map.Entry<T, String> option : options.entrySet()) {
      vbox.getChildren().add(Buttons.create(option.getValue(), e -> dialogPane.close(option.getKey())));
    }

    dialogPane.getChildren().add(vbox);

    return Optional.ofNullable(dialogPane.showDialog(((Node)event.getTarget()).getScene(), true));
  }

  public static <T> Optional<T> show(Event event, Map<T, String> options) {
    return show(event, null, options);
  }

  public static Optional<Integer> show(Event event, Node content, String... options) {
    return show(
      event,
      content,
      IntStream.range(0, options.length)
        .boxed()
        .collect(Collectors.toMap(Function.identity(), i -> options[i], (a, b) -> a, LinkedHashMap::new))
    );
  }

  public static Optional<Integer> show(Event event, String... options) {
    return show(event, null, options);
  }

  /**
   * Shows a dialog with a progress bar controlled by the supplied {@link Task}.  Once the
   * task finishes the dialog closes automatically and returns the result of the task.  If
   * the task can be cancelled and it was cancelled before the task finishes or the task threw
   * an exception, an empty {@link Optional} is returned.<p>
   *
   * If the task finished exceptionally, another dialog is shown with an error message.<p>
   *
   * Although the dialog is always shown and can be interacted with, it will remain
   * hidden (100% transparent) until 1 second has elapsed.  Short running tasks therefore
   * can remain hidden, while longer running tasks (or tasks that occasionally take longer)
   * will become visible to inform the user.<p>
   *
   * This can only be called with an {@link Event} which is associated with a {@link Scene}
   * and only from event handlers because of the nested event loop that will be constructed.
   *
   * @param <T> the result type of the {@link Task}
   * @param event an {@link Event} to determine where to show this dialog
   * @param cancellable whether or not the task can be cancelled
   * @param task a {@link Task} to execute, cannot be {@code null}
   * @return an Optional with the result of the {@link Task} or empty if the task was cancelled or threw an exception
   */
  public static <T> Optional<T> showProgressDialog(Event event, boolean cancellable, Task<T> task) {
    return showProgressDialog(((Node)event.getTarget()).getScene(), cancellable, task);
  }

  private static <T> Optional<T> showProgressDialog(Scene scene, boolean cancellable, Task<T> task) {
    if(task == null) {
      throw new IllegalArgumentException("task cannot be null");
    }

    DialogPane<T> dialogPane = new DialogPane<>("dialog", 1000, e -> {
      if(cancellable) {  // if task can be cancelled, then cancel it (which should close it)
        task.cancel(true);
      }

      return task.isDone();  // if task has completed, then navigating back should close it if it wasn't closed automatically (on failure it doesn't close automatically)
    });

    ProgressBar pb = new ProgressBar();

    dialogPane.getStylesheets().add(STYLES_URL);

    pb.progressProperty().bind(task.progressProperty());

    VBox progressBox = Containers.vbox(
      "progress-box",
      Labels.create("message", task.messageProperty(), Labels.HIDE_IF_EMPTY),
      pb
    );

    dialogPane.getChildren().add(Containers.vbox(
      "content",
      Labels.create("title", task.titleProperty(), Labels.HIDE_IF_EMPTY),
      progressBox
    ));

    task.onSucceededProperty().set(e -> dialogPane.close(task.getValue()));
    task.onCancelledProperty().set(e -> dialogPane.close());
    task.onFailedProperty().set(e -> {
      task.getException().printStackTrace();

      MarkdownTextView textView = new MarkdownTextView();
      String markdownText = translateException(task.getException());

      textView.markdownText.set(markdownText);
      textView.setMinWidth(progressBox.getScene().getWidth() / 2);
      textView.setMinHeight(progressBox.getScene().getHeight() / 4);

      progressBox.getChildren().clear();
      progressBox.getChildren().add(Containers.hbox(
        "result-box",
        Labels.create("exclamation", "!", Labels.REVERSE_CLIP_TEXT),
        textView
      ));
    });

    Thread thread = new Thread(task);

    thread.setName("Dialog-Task");
    thread.setDaemon(true);
    thread.start();  // If the task is really quick, it may close the dialog without ever showing it... Dialog allows this, and will just return whatever was provided when close methods are called

    return Optional.ofNullable(dialogPane.showDialog(scene, true));
  }

  public static <T> Optional<T> showProgressDialog(Event event, Task<T> task) {
    return showProgressDialog(event, true, task);
  }

  public static String translateException(Throwable t) {
    if(t instanceof Localizable l) {
      return l.toLocalizedString();
    }

    return "### Unexpected error\n"
        + "MediaSystem could not complete the current action because of "
        + "an internal error.\n"
        + "#### Technical details\n"
        + "```\n"
        + Throwables.toString(t)
        + "```\n";
  }
}
