package hs.mediasystem.framework.actions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javafx.event.Event;

public class ExposedMethod implements ExposedMember {
  private final Member member;
  private final String name;

  public ExposedMethod(Member member, String name) {
    this.member = member;
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Member getMember() {
    return member;
  }

  @Override
  public void doAction(String action, Object parent, Event event) {
    if(action.equals("trigger")) {
      try {
        Method method = getMember().getMethod();

        if(method.getParameterCount() == 1) {
          method.invoke(parent, event);
        }
        else {
          method.invoke(parent);
        }
      }
      catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new IllegalStateException("action=" + action + ", member=" + member + ", object=" + parent + ", event=" + event, e);
      }
    }
    else {
      throw new IllegalArgumentException("Unknown action '" + action + "' for: " + getMember());
    }
  }
}
