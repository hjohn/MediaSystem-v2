package hs.mediasystem.domain.stream;

import java.util.Objects;

public class StreamID {
  private final int id;

  public StreamID(int id) {
    this.id = id;
  }

  public int asInt() {
    return id;
  }

  @Override
  public String toString() {
    return "StreamID(" + id + ")";
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

    StreamID other = (StreamID)obj;

    return id == other.id;
  }
}
