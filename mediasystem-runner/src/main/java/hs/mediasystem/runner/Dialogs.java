package hs.mediasystem.runner;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.javafx.Buttons;
import hs.mediasystem.util.javafx.Labels;

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

  public static <T> Optional<T> show(Event event, Map<T, String> options) {
    DialogPane<T> dialogPane = new DialogPane<>();
    VBox vbox = new VBox();

    for(Map.Entry<T, String> option : options.entrySet()) {
      vbox.getChildren().add(Buttons.create(option.getValue(), e -> dialogPane.close(option.getKey())));
    }

    dialogPane.getChildren().add(vbox);

    return Optional.ofNullable(dialogPane.showDialog(((Node)event.getTarget()).getScene(), true).orElse(null));
  }

  public static Optional<Integer> show(Event event, String... options) {
    return show(
      event,
      IntStream.range(0, options.length).boxed().collect(Collectors.toMap(Function.identity(), i -> options[i]))
    );
  }

  public static <T> Exceptional<T> showProgressDialog(Event event, Task<T> task) {
    DialogPane<T> dialogPane = new DialogPane<>(1000);
    VBox vbox = new VBox();

    vbox.getChildren().add(Labels.create("title", task.titleProperty()));
    vbox.getChildren().add(Labels.create("message", task.messageProperty()));

    VBox resultBox = new VBox();
    ProgressIndicator pb = new ProgressIndicator();

    pb.progressProperty().bind(task.progressProperty());

    resultBox.getChildren().add(pb);

    vbox.getChildren().add(resultBox);

    dialogPane.getChildren().add(vbox);

    task.onFailedProperty().set(e -> {
      task.getException().printStackTrace();

      resultBox.getChildren().clear();
      resultBox.getChildren().add(Labels.create(task.getException().toString(), "error"));
    });
    task.onSucceededProperty().set(e -> dialogPane.close(task.getValue()));

    Thread thread = new Thread(task);

    thread.setName("Dialogs");
    thread.setDaemon(true);
    thread.start();  // If the task is really quick, it may close the dialog without ever showing it... Dialog allows this, and will just return whatever was provided when close methods are called

    return dialogPane.showDialog(((Node)event.getTarget()).getScene(), true);
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
