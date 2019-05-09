package hs.mediasystem.runner;

import hs.mediasystem.framework.expose.ExposedControl;
import hs.mediasystem.framework.expose.ExposedMethod;
import hs.mediasystem.framework.expose.ExposedProperty;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

@Singleton
public class ActionTargetProvider {
// TODO just roll this into Expose2
  public List<ActionTarget> getActionTargets(Class<?> rootClass) {
    return createActionTargets(rootClass, new ArrayList<>());
  }

  private List<ActionTarget> createActionTargets(Class<?> rootClass, List<ExposedControl<?>> currentPath) {
    List<ActionTarget> actionTargets = new ArrayList<>();

    while(rootClass != null) {
      for(ExposedControl<?> exposedControl : ExposedControl.find(rootClass)) {
        currentPath.add(exposedControl);

        if(exposedControl instanceof ExposedProperty) {
          ExposedProperty<?, ?> exposedProperty = (ExposedProperty<?, ?>)exposedControl;

          if(exposedProperty.isProvidingSubType()) {
            actionTargets.addAll(createActionTargets(exposedProperty.getProvidedType(), currentPath));
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
