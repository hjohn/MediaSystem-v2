package hs.mediasystem.ui.api.domain;

public class Role {
  public enum Type {CAST, CREW, GUEST_STAR}

  private final Type type;
  private final String character;
  private final String job;

  public Role(Type type, String character, String job) {
    this.type = type;
    this.character = character;
    this.job = job;
  }

  public Type getType() {
    return type;
  }

  public String getCharacter() {
    return character;
  }

  public String getJob() {
    return job;
  }
}
