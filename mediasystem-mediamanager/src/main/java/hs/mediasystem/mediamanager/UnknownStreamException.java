package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.scanner.api.BasicStream;

import java.util.Objects;

public class UnknownStreamException extends RuntimeException {
  private final BasicStream stream;
  private final IdentificationService idService;

  public UnknownStreamException(BasicStream stream, IdentificationService idService) {
    super("Unable to identify " + stream + " with " + idService);

    if(stream == null) {
      throw new IllegalArgumentException("stream cannot be null");
    }
    if(idService == null) {
      throw new IllegalArgumentException("idService cannot be null");
    }

    this.stream = stream;
    this.idService = idService;
  }

  @Override
  public int hashCode() {
    return Objects.hash(stream, idService);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    UnknownStreamException other = (UnknownStreamException)obj;

    if(!idService.equals(other.idService)) {
      return false;
    }
    if(!stream.equals(other.stream)) {
      return false;
    }

    return true;
  }
}
