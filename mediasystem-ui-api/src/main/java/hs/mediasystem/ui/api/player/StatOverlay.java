package hs.mediasystem.ui.api.player;

public class StatOverlay {
  public static final StatOverlay DISABLED = new StatOverlay(-1, "Disabled");

  private final int id;
  private final String description;

  public StatOverlay(int id, String description) {
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

    StatOverlay other = (StatOverlay) obj;

    return id == other.id;
  }

  @Override
  public String toString() {
    return "('" + description + "', StatOverlay[id=" + id + "])";
  }
}
