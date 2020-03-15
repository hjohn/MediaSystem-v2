package hs.mediasystem.runner;

import hs.mediasystem.util.expose.AbstractExposedProperty;
import hs.mediasystem.util.expose.ExposedControl;
import hs.mediasystem.util.expose.ExposedMethod;
import hs.mediasystem.util.expose.ExposedNode;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

@Singleton
public class ActionTargetProvider {

  public List<ActionTarget> getActionTargets(Class<?> rootClass) {
    return createActionTargets(rootClass, new ArrayList<>());
  }

  private List<ActionTarget> createActionTargets(Class<?> rootClass, List<ExposedControl<?>> currentPath) {
    List<ActionTarget> actionTargets = new ArrayList<>();

    while(rootClass != null) {
      for(ExposedControl<?> exposedControl : ExposedControl.find(rootClass)) {
        currentPath.add(exposedControl);

        if(exposedControl instanceof AbstractExposedProperty) {
          if(exposedControl instanceof ExposedNode) {
            actionTargets.addAll(createActionTargets(((ExposedNode<?, ?>)exposedControl).getProvidedType(), currentPath));
          }
          else {
            actionTargets.add(new ActionTarget(currentPath));
          }
        }
        else if(exposedControl instanceof ExposedMethod) {
          actionTargets.add(new ActionTarget(currentPath));
        }
        else {
          throw new IllegalStateException("Unhandled exposed member: " + exposedControl);
        }

        currentPath.remove(currentPath.size() - 1);
      }

      rootClass = rootClass.getSuperclass();
    }

    return actionTargets;
  }
}
