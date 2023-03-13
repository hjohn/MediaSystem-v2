package hs.mediasystem.runner;

import hs.mediasystem.runner.util.action.ActionTarget;
import hs.mediasystem.util.expose.ExposedControl;
import hs.mediasystem.util.expose.ExposedNode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.int4.dirk.util.Types;

@Singleton
public class ActionTargetProvider {

  public List<ActionTarget> getActionTargets(Class<?> rootClass) {
    return getAllActionTargets(rootClass, null).toList();
  }

  public Optional<ActionTarget> findMatching(Class<?> rootClass, String path) {
    return getAllActionTargets(rootClass, null)  // TODO result of this call is basically constant (if it wasn't a stream)
      .filter(at -> at.toPath().equals(path))
      .findFirst();
  }

  private Stream<ActionTarget> getAllActionTargets(Class<?> rootClass, ActionTarget parent) {
    return Types.getSuperTypes(rootClass).stream()
      .map(ExposedControl::find)
      .flatMap(Collection::stream)
      .flatMap(control -> toActionTarget(control, parent));
  }

  private Stream<ActionTarget> toActionTarget(ExposedControl exposedControl, ActionTarget parent) {
    ActionTarget actionTarget = new ActionTarget(parent, exposedControl);

    return exposedControl instanceof ExposedNode<?> exposedNode
      ? getAllActionTargets(exposedNode.getProvidedType(), actionTarget)
      : Stream.of(actionTarget);
  }
}
