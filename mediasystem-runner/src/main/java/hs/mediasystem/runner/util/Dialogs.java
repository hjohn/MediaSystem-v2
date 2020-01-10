package hs.mediasystem.runner.util;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.util.javafx.control.Buttons;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

public class Dialogs {

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

  public static <T> Optional<T> showProgressDialog(Event event, Task<T> task) {
    DialogPane<T> dialogPane = new DialogPane<>("dialog", 1000);
    ProgressIndicator pb = new ProgressIndicator();

    pb.progressProperty().bind(task.progressProperty());

    VBox resultBox = Containers.vbox(pb);

    dialogPane.getChildren().add(Containers.vbox(
      Labels.create("title", task.titleProperty()),
      Labels.create("message", task.messageProperty()),
      resultBox
    ));

    task.onFailedProperty().set(e -> {
      task.getException().printStackTrace();

      resultBox.getChildren().clear();
      resultBox.getChildren().add(Labels.create("error", task.getException().toString()));
    });
    task.onSucceededProperty().set(e -> dialogPane.close(task.getValue()));

    Thread thread = new Thread(task);

    thread.setName("Dialog-Task");
    thread.setDaemon(true);
    thread.start();  // If the task is really quick, it may close the dialog without ever showing it... Dialog allows this, and will just return whatever was provided when close methods are called

    return Optional.ofNullable(dialogPane.showDialog(((Node)event.getTarget()).getScene(), true));
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
