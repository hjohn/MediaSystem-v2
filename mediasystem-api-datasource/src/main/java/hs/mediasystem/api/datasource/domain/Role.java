package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.RoleId;

public class Role {
  public enum Type {CAST, CREW, GUEST_STAR}

  private final RoleId id;
  private final Type type;
  private final String department;
  private final String job;
  private final String character;

  public static Role asCast(RoleId id, String character) {
    return new Role(id, Type.CAST, null, null, character);
  }

  public static Role asGuestStar(RoleId id, String character) {
    if(character == null) {
      throw new IllegalArgumentException("character cannot be null");
    }

    return new Role(id, Type.GUEST_STAR, null, null, character);
  }

  public static Role asCrew(RoleId id, String department, String job) {
    if(department == null) {
      throw new IllegalArgumentException("department cannot be null");
    }
    if(job == null) {
      throw new IllegalArgumentException("job cannot be null");
    }

    return new Role(id, Type.CREW, department, job, null);
  }

  private Role(RoleId id, Type type, String department, String job, String character) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }

    this.id = id;
    this.type = type;
    this.department = department;
    this.job = job;
    this.character = character;
  }

  public RoleId getId() {
    return id;
  }

  public Type getType() {
    return type;
  }

  public String getDepartment() {
    return department;
  }

  public String getJob() {
    return job;
  }

  public String getCharacter() {
    return character;
  }
}
