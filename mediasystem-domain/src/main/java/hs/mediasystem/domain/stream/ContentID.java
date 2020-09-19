package hs.mediasystem.domain.stream;

import java.util.Objects;

/**
 * Unique identifier for a stream based on its content.  The same streams
 * will have the same identifier, even if they have different names.
 */
public class ContentID {
  private final int id;

  public ContentID(int id) {
    this.id = id;
  }

  public int asInt() {
    return id;
  }

  @Override
  public String toString() {
    return "ContentID(" + id + ")";
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ContentID other = (ContentID)obj;

    return id == other.id;
  }
}
