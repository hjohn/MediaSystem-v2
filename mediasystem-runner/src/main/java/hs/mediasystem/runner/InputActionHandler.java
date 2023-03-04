package hs.mediasystem.runner;

import hs.mediasystem.util.expose.ExposedControl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;

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
  @Inject private ActionTargetProvider actionTargetProvider;

  private final Map<KeyCombination, List<Action>> actions = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    createMappings();
  }

  public List<Action> findActions(KeyCombination combination) {
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

      @SuppressWarnings("unchecked")
      List<String> actionNames = inputMappings.get(key) instanceof List ? (List<String>)inputMappings.get(key) : List.of(inputMappings.get(key).toString());

      for(String action : actionNames) {
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

  private static String createPath(List<ExposedControl> path) {
    StringBuilder sb = new StringBuilder();

    for(ExposedControl member : path) {
      if(sb.length() == 0) {
        sb.append(member.getDeclaringClass().getName());
      }

      sb.append(".").append(member.getName());
    }

    return sb.toString();
  }
}
