package hs.mediasystem.runner.util;

import hs.mediasystem.util.Localizable;
import hs.mediasystem.util.javafx.control.Buttons;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class Dialogs {
  private static final LessLoader LESS_LOADER = new LessLoader(Dialogs.class);

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
      IntStream.range(0, options.length).boxed().collect(Collectors.toMap(Function.identity(), i -> options[i]))
    );
  }

  public static Optional<Integer> show(Event event, String... options) {
    return show(event, null, options);
  }

  public static <T> Optional<T> showProgressDialog(Event event, boolean closable, Task<T> task) {
    DialogPane<T> dialogPane = new DialogPane<>("dialog", 1000);
    ProgressBar pb = new ProgressBar();

    dialogPane.getStylesheets().add(LESS_LOADER.compile("dialogs.less"));

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
    task.onSucceededProperty().set(e -> dialogPane.close(task.getValue()));

    Thread thread = new Thread(task);

    thread.setName("Dialog-Task");
    thread.setDaemon(true);
    thread.start();  // If the task is really quick, it may close the dialog without ever showing it... Dialog allows this, and will just return whatever was provided when close methods are called

    return Optional.ofNullable(dialogPane.showDialog(((Node)event.getTarget()).getScene(), true, closable));
  }

  public static <T> Optional<T> showProgressDialog(Event event, Task<T> task) {
    return showProgressDialog(event, true, task);
  }

  public static String translateException(Throwable t) {
    if(t instanceof Localizable) {
      return ((Localizable)t).toLocalizedString();
    }

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    t.printStackTrace(printWriter);

    return "### Unexpected error\n"
        + "MediaSystem could not complete the current action because of "
        + "an internal error.  Technical details:\n\n"
        + "```\n"
        + stringWriter.toString()
        + "```\n";
  }
}
