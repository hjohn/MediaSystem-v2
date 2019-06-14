package hs.mediasystem.runner;

import hs.mediasystem.framework.expose.ExposedControl;
import hs.mediasystem.runner.util.Dialogs;
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.input.KeyEvent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class InputActionHandler {
  private static final Logger LOGGER = Logger.getLogger(InputActionHandler.class.getName());

  @Inject private Ini ini;
  @Inject private ActionTargetProvider actionTargetProvider;

  private final Map<KeyCombination, List<Action>> actions = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    createMappings();
  }

  public List<Action> findActions(KeyCombination combination) {
    LOGGER.fine("Findactions for " + combination);
    List<Action> list = actions.get(combination);

    return list == null ? Collections.emptyList() : list;
  }

  public void keyPressed(Event event, Object root, List<Action> actions) {
    for(Action action : actions) {
      ActionTarget actionTarget = action.getActionTarget();

      if(actionTarget.getActionClass().isAssignableFrom(root.getClass())) {
        Task<Object> task = actionTarget.doAction(action.getAction(), root, event);

        if(task != null) {
          // Action only returned a Task, that must be executed asynchronously (otherwise it was already completed on FX thread).
          Dialogs.showProgressDialog(event, task);
        }
      }
    }
  }

  public static KeyCodeCombination keyEventToKeyCodeCombination(KeyEvent event, boolean longPress) {
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

  private void createMappings() {
    actions.clear();

    Section section = ini.getSection("input-mappings");

    for(String key : section) {
      KeyCombination combination = KeyCombination.valueOf(key);

      if(combination instanceof KeyCharacterCombination) {
        LOGGER.warning("Invalid input mapping: " + key);
        continue;
      }

      LOGGER.info("Input Mapping Key '" + key + "' converted to valid key combination: " + combination);

      for(String action : section.getAll(key)) {
        String[] parts = action.split(":");

        try {
          Class<?> cls = Class.forName(parts[0]);

          for(ActionTarget actionTarget : actionTargetProvider.getActionTargets(cls)) {
            if(createPath(actionTarget.getPath()).equals(parts[0] + "." + parts[1])) {
              Action actionObject = new Action(actionTarget, parts.length > 2 ? parts[2] : "trigger");

              LOGGER.info("Mapped <" + combination + "> to " + actionObject);

              actions.computeIfAbsent(combination, k -> new ArrayList<>()).add(actionObject);
            }
          }
        }
        catch(ClassNotFoundException e) {
          LOGGER.warning("Unknown action (class not found) for input mapping: " + key + ", action was: " + action);
        }
      }
    }
  }

  private static String createPath(List<ExposedControl<?>> path) {
    StringBuilder sb = new StringBuilder();

    for(ExposedControl<?> member : path) {
      if(sb.length() == 0) {
        sb.append(member.getDeclaringClass().getName());
      }

      sb.append(".").append(member.getName());
    }

    return sb.toString();
  }


  public static class Action {
    private final ActionTarget actionTarget;
    private final String action;

    public Action(ActionTarget actionTarget, String action) {
      this.actionTarget = actionTarget;
      this.action = action;
    }

    public ActionTarget getActionTarget() {
      return actionTarget;
    }

    public String getAction() {
      return action;
    }

    @Override
    public String toString() {
      return actionTarget + "#" + action;
    }
  }
}
