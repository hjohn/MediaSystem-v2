package hs.mediasystem.framework.actions;

import javafx.event.Event;

public interface ExposedMember {

  String getName();

  Member getMember();

  /**
   * Triggers the given action on this member.
   *
   * @param action an action to trigger
   * @param parent an instance of a parent containing this member
   * @param event an {@link Event}
   */
  void doAction(String action, Object parent, Event event);
}