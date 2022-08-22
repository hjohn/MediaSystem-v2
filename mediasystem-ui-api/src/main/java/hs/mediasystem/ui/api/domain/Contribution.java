package hs.mediasystem.ui.api.domain;

public record Contribution(Person person, Role role, double order) {
  public Contribution {
    if(person == null) {
      throw new IllegalArgumentException("person cannot be null");
    }
    if(role == null) {
      throw new IllegalArgumentException("role cannot be null");
    }
  }
}