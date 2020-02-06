package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;

import java.util.Objects;

public class UnknownStreamableException extends RuntimeException {
  private final Streamable streamable;
  private final IdentificationService idService;

  public UnknownStreamableException(Streamable streamable, IdentificationService idService) {
    super("Unable to identify " + streamable + " with " + idService);

    if(streamable == null) {
      throw new IllegalArgumentException("streamable cannot be null");
    }
    if(idService == null) {
      throw new IllegalArgumentException("idService cannot be null");
    }

    this.streamable = streamable;
    this.idService = idService;
  }

  @Override
  public int hashCode() {
    return Objects.hash(streamable, idService);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    UnknownStreamableException other = (UnknownStreamableException)obj;

    if(!idService.equals(other.idService)) {
      return false;
    }
    if(!streamable.equals(other.streamable)) {
      return false;
    }

    return true;
  }
}
