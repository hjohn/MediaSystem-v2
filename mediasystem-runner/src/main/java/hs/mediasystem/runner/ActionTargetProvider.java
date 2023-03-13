package hs.mediasystem.runner;

import hs.mediasystem.runner.util.action.ActionTarget;
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
    return createActionTargets(rootClass, null);
  }

  private List<ActionTarget> createActionTargets(Class<?> rootClass, ActionTarget parent) {
    List<ActionTarget> actionTargets = new ArrayList<>();

    while(rootClass != null) {
      for(ExposedControl exposedControl : ExposedControl.find(rootClass)) {
        ActionTarget actionTarget = new ActionTarget(parent, exposedControl);

        if(exposedControl instanceof AbstractExposedProperty) {
          if(exposedControl instanceof ExposedNode<?> en) {
            actionTargets.addAll(createActionTargets(en.getProvidedType(), actionTarget));
          }
          else {
            actionTargets.add(actionTarget);
          }
        }
        else if(exposedControl instanceof ExposedMethod) {
          actionTargets.add(actionTarget);
        }
        else {
          throw new IllegalStateException("Unhandled exposed member: " + exposedControl);
        }
      }

      rootClass = rootClass.getSuperclass();
    }

    return actionTargets;
  }
}
