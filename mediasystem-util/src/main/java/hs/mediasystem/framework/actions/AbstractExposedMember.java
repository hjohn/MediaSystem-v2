package hs.mediasystem.framework.actions;

import javafx.beans.property.Property;
import javafx.event.Event;

public abstract class AbstractExposedMember<T extends Property<?>> implements ExposedMember {
  private final Member member;
  private final String name;

  public AbstractExposedMember(Member member, String name) {
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

  @SuppressWarnings("unchecked")
  @Override
  public final void doAction(String action, Object parent, Event event) {
    doAction(action, parent, (T)getMember().get(parent), event);
  }

  public abstract void doAction(String action, Object parent, T property, Event event);

  @Override
  public String toString() {
    return member.getDeclaringClass().getName() + "::" + member.getName();
  }
}