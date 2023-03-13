package hs.mediasystem.runner;

import hs.mediasystem.presentation.NavigateEvent;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.PresentationActionEvent;
import hs.mediasystem.presentation.PresentationEvent;
import hs.mediasystem.runner.dialog.Dialogs;
import hs.mediasystem.runner.util.SceneManager;
import hs.mediasystem.runner.util.action.Action;
import hs.mediasystem.util.expose.Trigger;
import hs.mediasystem.util.javafx.base.Events;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCombination.Modifier;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RootPresentationHandler implements EventRoot {
  @Inject private SceneManager sceneManager;
  @Inject private InputActionHandler inputActionHandler;
  @Inject private ContextMenuHandler contextMenuHandler;
  @Inject private ActionTargetProvider actionTargetProvider;

  private KeyCode keyPressedCode;
  private long keyPressedStartTime;

  @PostConstruct
  private void postConstruct() {
    Scene scene = sceneManager.getScene();

    scene.setOnKeyPressed(this::onKeyPressed);
    scene.setOnKeyReleased(this::onKeyReleased);
    scene.setOnMouseClicked(this::onMouseClicked);

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

    IntStream.range(0, presentations.size())
      .mapToObj(i -> presentations.get(presentations.size() - i - 1))
      .map(p -> toTrigger(p, event.getAction()))
      .flatMap(Optional::stream)
      .takeWhile(x -> !event.isConsumed())
      .forEach(trigger -> trigger.run(event, task -> Dialogs.showProgressDialog(event, task)));
  }

  private Optional<Trigger<Object>> toTrigger(Presentation presentation, Action action) {
    return actionTargetProvider.findMatching(presentation.getClass(), action.getPath())
      .map(actionTarget -> actionTarget.createTrigger(action.getDescriptor(), presentation));  // can result in empty optional when createTrigger returns null
  }

  private void onKeyReleased(KeyEvent event) {
    if(event.getCode().isModifierKey()) {
      return;
    }

    if(event.getCode().isFunctionKey() && keyPressedCode == event.getCode()) {
      long heldTime = System.currentTimeMillis() - keyPressedStartTime;

      if(heldTime >= 0 && heldTime <= 500) {
        handleKeyEvent(event, false);
      }

      keyPressedCode = null;
    }
  }

  private void onKeyPressed(KeyEvent event) {
    if(event.getCode().isModifierKey()) {
      return;
    }

    if(event.getCode().isFunctionKey()) {
      // Special handling of Context Menu key
      if(event.getCode() == KeyCode.F10) {
        fire(PresentationEvent.triggerContextMenu());

        event.consume();

        return;
      }

      if(keyPressedCode != event.getCode()) {
        keyPressedCode = event.getCode();
        keyPressedStartTime = System.currentTimeMillis();
      }

      long heldTime = System.currentTimeMillis() - keyPressedStartTime;

      if(keyPressedCode == event.getCode() && heldTime >= 700) {
        keyPressedStartTime = Long.MAX_VALUE;  // prevents repeating long press keys

        handleKeyEvent(event, true);
      }

      return;
    }

    handleKeyEvent(event, false);
  }

  private void onMouseClicked(MouseEvent event) {
    if(event.getButton() == MouseButton.SECONDARY) {
      Event.fireEvent(event.getTarget(), NavigateEvent.back());

      event.consume();
    }
  }

  private void handleKeyEvent(KeyEvent event, boolean longPress) {
    List<Action> actions = inputActionHandler.findActions(keyEventToKeyCodeCombination(event, longPress));

    for(Action action : actions) {
      if(Events.dispatchEvent(event.getTarget(), PresentationActionEvent.createActionProposal(action))) {
        event.consume();
        break;  // action was consumed, don't process potential other actions
      }
    }
  }

  private static KeyCodeCombination keyEventToKeyCodeCombination(KeyEvent event, boolean longPress) {
    List<Modifier> modifiers = new ArrayList<>();

    if(event.isControlDown()) {
      modifiers.add(KeyCombination.CONTROL_DOWN);
    }
    if(event.isAltDown() || longPress) {
      modifiers.add(KeyCombination.ALT_DOWN);
    }
    if(event.isShiftDown()) {
      modifiers.add(KeyCombination.SHIFT_DOWN);
    }
    if(event.isMetaDown()) {
      modifiers.add(KeyCombination.META_DOWN);
    }

    return new KeyCodeCombination(event.getCode(), modifiers.toArray(new Modifier[modifiers.size()]));
  }
}
