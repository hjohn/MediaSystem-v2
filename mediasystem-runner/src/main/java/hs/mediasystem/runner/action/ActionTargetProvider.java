package hs.mediasystem.runner.action;

import hs.mediasystem.runner.util.action.ActionTarget;
import hs.mediasystem.util.expose.ExposedControl;
import hs.mediasystem.util.expose.ExposedNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import org.int4.dirk.util.Types;

@Singleton
public class ActionTargetProvider {
  private final Map<Class<?>, List<ActionTarget>> actionTargets = new HashMap<>();

  public List<ActionTarget> getActionTargets(Class<?> rootClass) {
    return actionTargets.computeIfAbsent(rootClass, k -> getAllActionTargets(rootClass, null));
  }

  public Optional<ActionTarget> findMatching(Class<?> rootClass, String path) {
    return getActionTargets(rootClass)
      .stream()
      .filter(at -> at.toPath().equals(path))
      .findFirst();
  }

  private List<ActionTarget> getAllActionTargets(Class<?> rootClass, ActionTarget parent) {
    return Types.getSuperTypes(rootClass).stream()
      .map(ExposedControl::find)
      .flatMap(Collection::stream)
      .flatMap(control -> toActionTarget(control, parent).stream())
      .toList();
  }

  private List<ActionTarget> toActionTarget(ExposedControl exposedControl, ActionTarget parent) {
    ActionTarget actionTarget = new ActionTarget(parent, exposedControl);

    return exposedControl instanceof ExposedNode<?> exposedNode
      ? getAllActionTargets(exposedNode.getProvidedType(), actionTarget)
      : List.of(actionTarget);
  }
}
