package hs.mediasystem.framework.actions;

import javafx.beans.property.ObjectProperty;
import javafx.event.Event;

public class ExposedActionObjectProperty extends AbstractExposedMember<ObjectProperty<Object>> {
  private final ValueBuilder<Object> valueBuilder;

  public ExposedActionObjectProperty(Member member, String name, ValueBuilder<Object> valueBuilder) {
    super(member, name);

    this.valueBuilder = valueBuilder;
  }

  @Override
  public void doAction(String action, Object parent, ObjectProperty<Object> property, Event event) {
    if(action.equals("trigger")) {
      property.set(valueBuilder.build(event, property.get()));
    }
    else {
      throw new IllegalStateException("Unknown action '" + action + "' for: " + property);
    }
  }
}