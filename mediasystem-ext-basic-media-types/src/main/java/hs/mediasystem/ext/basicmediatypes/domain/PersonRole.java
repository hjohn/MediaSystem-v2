package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.domain.work.RoleId;

public class PersonRole {
  private final Person person;
  private final Role role;
  private final double order;

  public PersonRole(Person person, Role role, double order) {
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

  public RoleId getId() {
    return role.getId();
  }

  public Details getDetails() {
    return person.getDetails();
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

