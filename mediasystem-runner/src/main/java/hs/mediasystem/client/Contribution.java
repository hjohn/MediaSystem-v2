package hs.mediasystem.client;

import hs.mediasystem.ext.basicmediatypes.domain.Role;

public class Contribution {
  private final Person person;
  private final Role role;
  private final double order;

  public Contribution(Person person, Role role, double order) {
    if(person == null) {
      throw new IllegalArgumentException("person cannot be null");
    }
    if(role == null) {
      throw new IllegalArgumentException("role cannot be null");
    }

    this.person = person;
    this.role = role;
    this.order = order;
  }

  public Person getPerson() {
    return person;
  }

  public Role getRole() {
    return role;
  }

  public double getOrder() {
    return order;
  }
}