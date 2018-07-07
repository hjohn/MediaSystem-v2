package hs.mediasystem.domain;

public class Subtitle {
  public static final Subtitle DISABLED = new Subtitle(-1, "Disabled");

  private final int id;
  private final String description;

  public Subtitle(int id, String description) {
    this.id = id;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public int getId() {
    return id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Subtitle other = (Subtitle) obj;

    return id == other.id;
  }

  @Override
  public String toString() {
    return "('" + description + "', Subtitle[id=" + id + "])";
  }
}
