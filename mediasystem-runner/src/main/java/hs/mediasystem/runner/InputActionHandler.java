package hs.mediasystem.runner;

import hs.mediasystem.presentation.NavigateEvent;
import hs.mediasystem.presentation.PresentationActionEvent;
import hs.mediasystem.presentation.PresentationEvent;
import hs.mediasystem.runner.util.action.Action;
import hs.mediasystem.util.javafx.base.Events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCombination.Modifier;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Responsible for parsing key to action mappings, and converting {@link KeyCombination}s
 * to {@link Action}s.
 */
@Singleton
public class InputActionHandler {
  private static final Logger LOGGER = Logger.getLogger(InputActionHandler.class.getName());

  @Inject @Named("input-mappings") private Map<String, Object> inputMappings;

  private final Map<KeyCombination, List<Action>> actions = new HashMap<>();

  private KeyCode keyPressedCode;
  private long keyPressedStartTime;

  @PostConstruct
  private void postConstruct() {
    createMappings();
  }

  public void attachToScene(Scene scene) {
    scene.setOnKeyPressed(this::onKeyPressed);
    scene.setOnKeyReleased(this::onKeyReleased);
    scene.setOnMouseClicked(this::onMouseClicked);
  }

  private List<Action> findActions(KeyCombination combination) {
    List<Action> list = actions.getOrDefault(combination, List.of());

    LOGGER.fine("Findactions for " + combination + " -> " + list);

    return list;
  }

  private void createMappings() {
    actions.clear();

    for(String key : inputMappings.keySet()) {
      KeyCombination combination = KeyCombination.valueOf(key);

      if(combination instanceof KeyCharacterCombination) {
        LOGGER.warning("Invalid input mapping: " + key);
        continue;
      }

      LOGGER.fine("Input Mapping Key '" + key + "' converted to valid key combination: " + combination);

      Object object = inputMappings.get(key);

      @SuppressWarnings("unchecked")
      List<String> actionNames = object instanceof List ? (List<String>)object : List.of(object.toString());

      for(String action : actionNames) {
        String[] parts = action.split(":");
        Action actionObject = new Action(parts[0], parts.length > 1 ? parts[1] : "trigger");

        actions.computeIfAbsent(combination, k -> new ArrayList<>()).add(actionObject);
      }
    }
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
        if(event.getSource() instanceof Node node) {
          node.fireEvent(PresentationEvent.triggerContextMenu());
        }

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
    List<Action> actions = findActions(keyEventToKeyCodeCombination(event, longPress));

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
