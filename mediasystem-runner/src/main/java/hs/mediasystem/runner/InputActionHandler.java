package hs.mediasystem.runner;

import hs.mediasystem.runner.util.action.Action;

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
        Action actionObject = new Action(parts[0], parts.length > 1 ? parts[1] : "trigger");

        actions.computeIfAbsent(combination, k -> new ArrayList<>()).add(actionObject);
      }
    }
  }
}
