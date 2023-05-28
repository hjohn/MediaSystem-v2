package hs.mediasystem.runner;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.PresentationActionEvent;
import hs.mediasystem.presentation.PresentationActionFiredEvent;
import hs.mediasystem.presentation.PresentationEvent;
import hs.mediasystem.runner.action.ActionTargetProvider;
import hs.mediasystem.runner.action.ContextMenuHandler;
import hs.mediasystem.runner.action.InputActionHandler;
import hs.mediasystem.runner.dialog.Dialogs;
import hs.mediasystem.runner.util.SceneManager;
import hs.mediasystem.runner.util.action.Action;
import hs.mediasystem.runner.util.action.ActionTarget;
import hs.mediasystem.util.expose.Trigger;
import hs.mediasystem.util.javafx.base.Events;

import java.util.List;
import java.util.stream.Collectors;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.Scene;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RootPresentationHandler implements EventRoot {
  @Inject private SceneManager sceneManager;
  @Inject private InputActionHandler inputActionHandler;
  @Inject private ContextMenuHandler contextMenuHandler;
  @Inject private ActionTargetProvider actionTargetProvider;

  @PostConstruct
  private void postConstruct() {
    Scene scene = sceneManager.getScene();

    inputActionHandler.attachToScene(scene);

    scene.addEventHandler(PresentationEvent.CONTEXT_MENU, contextMenuHandler::handle);
    scene.addEventHandler(PresentationEvent.REFRESH, RootPresentationHandler::refresh);
    scene.addEventHandler(PresentationEvent.REQUEST_FOCUSED_REFRESH, this::refreshFocused);
    scene.addEventHandler(PresentationActionEvent.PROPOSED, this::handleActionEvent);

    sceneManager.getRootPane().getStyleClass().setAll("root", "media-look");
  }

  @Override
  public void fire(Event event) {
    sceneManager.getScene().getFocusOwner().fireEvent(event);
  }

  private void refreshFocused(Event event) {
    fire(PresentationEvent.refresh());
    event.consume();
  }

  private static void refresh(PresentationEvent event) {
    Task<List<Runnable>> refreshTask = new Task<>() {
      @Override
      protected List<Runnable> call() throws Exception {
        updateTitle("Refreshing...");

        return event.getPresentations().stream()
          .map(Presentation::createUpdateTask)
          .collect(Collectors.toList());
      }
    };

    Dialogs.showProgressDialog(event, refreshTask)
      .ifPresent(list -> list.stream().forEach(Runnable::run));
  }

  private void handleActionEvent(PresentationActionEvent event) {
    List<Presentation> presentations = event.getPresentations();

    for(int i = presentations.size(); i-- > 0; ) {
      Presentation p = presentations.get(i);

      if(tryRunAction(event, p)) {
        break;
      }
    }
  }

  private boolean tryRunAction(PresentationActionEvent event, Presentation presentation) {
    Action action = event.getAction();
    ActionTarget actionTarget = actionTargetProvider.findMatching(presentation.getClass(), action.getPath()).orElse(null);

    if(actionTarget == null) {
      return false;
    }

    Trigger<Object> trigger = actionTarget.createTrigger(action.getDescriptor(), presentation);

    trigger.run(event, task -> Dialogs.showProgressDialog(event, task));

    if(!event.isConsumed()) {
      return false;
    }

    Events.dispatchEvent(event.getTarget(), PresentationActionFiredEvent.create(action, actionTarget, presentation));

    return true;
  }
}
