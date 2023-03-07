package hs.mediasystem.presentation;

import hs.mediasystem.runner.ActionTarget;
import hs.mediasystem.runner.util.Dialogs;
import hs.mediasystem.util.expose.Trigger;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.Node;

public class Presentations {
  private static final Logger LOGGER = Logger.getLogger(Presentations.class.getName());

  /**
   * Associates a {@link Presentation} with a given {@link Node} so that it may
   * process {@link PresentationEvent}s relevant to it
   *
   * @param node a {@link Node}, cannot be {@code null}
   * @param presentation a {@link Presentation}, cannot be {@code null}
   * @throws NullPointerException when either the given node or presentation are {@code null}
   */
  public static void associate(Node node, Presentation presentation) {
    node.addEventHandler(PresentationActionEvent.PROPOSED, e -> handleActionProposal(e, presentation));
    node.addEventFilter(PresentationEvent.ANY, e -> e.addPresentation(presentation));
  }

  public static void showWindow(Event event, Node content) {
    Dialogs.show(event, "", content);

    if(event.getTarget() instanceof Node node) {
      node.fireEvent(PresentationEvent.refresh());
    }
  }

  public static void showDialog(Event event, Node content) {
    Dialogs.show(event, content);

    if(event.getTarget() instanceof Node node) {
      node.fireEvent(PresentationEvent.refresh());
    }
  }

  public static void refresh(PresentationEvent event) {
    Task<List<Runnable>> refreshTask = new Task<>() {
      @Override
      protected List<Runnable> call() throws Exception {
        updateTitle("Refreshing...");

        return event.getPresentations().stream()
          .peek(p -> LOGGER.fine("Refreshing: " + p))
          .map(Presentation::createUpdateTask)
          .collect(Collectors.toList());
      }
    };

    Dialogs.showProgressDialog(event, refreshTask)
      .ifPresent(list -> list.stream().forEach(Runnable::run));
  }

  private static void handleActionProposal(PresentationActionEvent event, Presentation presentation) {
    ActionTarget actionTarget = event.getAction().getActionTarget();

    if(actionTarget.getActionClass().isAssignableFrom(presentation.getClass())) {
      Trigger<Object> trigger = actionTarget.createTrigger(event.getAction().getDescriptor(), presentation);

      if(trigger != null) {
        trigger.run(event, task -> Dialogs.showProgressDialog(event, task));
      }
    }
  }
}
