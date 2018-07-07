package hs.mediasystem.framework.actions;

import javafx.beans.property.BooleanProperty;
import javafx.event.Event;

public class ExposedBooleanProperty extends AbstractExposedMember<BooleanProperty> {

  public ExposedBooleanProperty(Member member, String name) {
    super(member, name);
  }

  @Override
  public void doAction(String action, Object parent, BooleanProperty property, Event event) {
    if(action.equals("toggle")) {
      property.setValue(!property.getValue());
    }
    else if(action.equals("set")) {
      property.setValue(true);
    }
    else if(action.equals("clear")) {
      property.setValue(false);
    }
    else {
      throw new IllegalArgumentException("Unknown action '" + action + "' for: " + property);
    }
  }
}