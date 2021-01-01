package hs.mediasystem.presentation;

import hs.mediasystem.runner.util.Dialogs;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.Node;

public class Presentations {
  private static final Logger LOGGER = Logger.getLogger(Presentations.class.getName());
  private static final String KEY = "presentation2";

  public static void showWindow(Event event, Node content) {
    Dialogs.show(event, "", content);

    if(event.getTarget() instanceof Node) {
      refreshAllTargetted(event);
    }
  }

  public static void showDialog(Event event, Node content) {
    Dialogs.show(event, content);

    if(event.getTarget() instanceof Node) {
      refreshAllTargetted(event);
    }
  }

  public static void refreshPresentations(Event event, List<Presentation> presentations) {
    Task<List<Runnable>> refreshTask = new Task<>() {
      @Override
      protected List<Runnable> call() throws Exception {
        updateTitle("Refreshing...");

        return presentations.stream()
          .peek(p -> LOGGER.fine("Refreshing: " + p))
          .map(Presentation::createUpdateTask)
          .collect(Collectors.toList());
      }
    };

    Dialogs.showProgressDialog(event, refreshTask)
      .ifPresent(list -> list.stream().forEach(Runnable::run));
  }

  private static void refreshAllTargetted(Event event) {
    refreshPresentations(event, createList((Node)event.getTarget()));
  }

  private static List<Presentation> createList(Node node) {
    return Stream.iterate(node, Objects::nonNull, Node::getParent)
      .map(s -> s.getProperties().get(KEY))
      .filter(Objects::nonNull)
      .map(Presentation.class::cast)
      .collect(Collectors.toList());
  }
}
