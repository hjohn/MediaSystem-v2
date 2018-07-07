package hs.mediasystem.ext.basicmediatypes.domain;

public class Role {
  public enum Type {CAST, CREW, GUEST_STAR}

  private final Type type;
  private final String department;
  private final String job;
  private final String character;

  public static Role asCast(String character) {
    if(character == null) {
      throw new IllegalArgumentException("character cannot be null");
    }

    return new Role(Type.CAST, null, null, character);
  }

  public static Role asGuestStar(String character) {
    if(character == null) {
      throw new IllegalArgumentException("character cannot be null");
    }

    return new Role(Type.GUEST_STAR, null, null, character);
  }

  public static Role asCrew(String department, String job) {
    if(department == null) {
      throw new IllegalArgumentException("department cannot be null");
    }
    if(job == null) {
      throw new IllegalArgumentException("job cannot be null");
    }

    return new Role(Type.CREW, department, job, null);
  }

  private Role(Type type, String department, String job, String character) {
    this.type = type;
    this.department = department;
    this.job = job;
    this.character = character;
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
