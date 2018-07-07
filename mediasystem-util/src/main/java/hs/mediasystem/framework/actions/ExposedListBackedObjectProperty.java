package hs.mediasystem.framework.actions;

import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;

public class ExposedListBackedObjectProperty extends AbstractExposedMember<ObjectProperty<Object>> {
  private final Member valuesMember;

  public ExposedListBackedObjectProperty(Member member, String name, Member valuesMember) {
    super(member, name);

    this.valuesMember = valuesMember;
  }

  @Override
  public void doAction(String action, Object parent, ObjectProperty<Object> property, Event event) {
    if(action.equals("next")) {
      @SuppressWarnings("unchecked")
      ObservableList<Object> list = (ObservableList<Object>)valuesMember.get(parent);
      int currentIndex = list.indexOf(property.get());

      currentIndex++;

      if(currentIndex >= list.size()) {
        currentIndex = 0;
      }

      property.set(list.get(currentIndex));
    }
    else {
      throw new IllegalStateException("Unknown action '" + action + "' for: " + property);
    }
  }
}