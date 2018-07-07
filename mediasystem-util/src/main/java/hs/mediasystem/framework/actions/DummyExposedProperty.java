package hs.mediasystem.framework.actions;

import javafx.event.Event;

public class DummyExposedProperty implements ExposedMember {
  private final Member member;
  private final String name;

  public DummyExposedProperty(Member member, String name) {
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
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return member.getDeclaringClass().getName() + "::" + member.getName();
  }
}