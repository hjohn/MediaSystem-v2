package hs.mediasystem.framework.actions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.Property;
import javafx.event.Event;

public class ExposedNumberProperty extends AbstractExposedMember<Property<Number>> {
  private static final Pattern PATTERN = Pattern.compile("(add|subtract)\\s*(?:\\((.+)\\))?");

  public ExposedNumberProperty(Member member, String name) {
    super(member, name);
  }

  @Override
  public void doAction(String action, Object parent, Property<Number> property, Event event) {
    Matcher matcher = PATTERN.matcher(action);

    if(matcher.matches()) {
      if(matcher.group(1).equals("add")) {
        property.setValue(property.getValue().doubleValue() + Double.parseDouble(matcher.group(2).trim()));
      }
      else if(matcher.group(1).equals("subtract")) {
        property.setValue(property.getValue().doubleValue() - Double.parseDouble(matcher.group(2).trim()));
      }
    }
    else {
      throw new IllegalStateException("Unknown action '" + action + "' for: " + property);
    }
  }
}
