package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;

import java.util.Objects;

public class UnknownStreamException extends RuntimeException {
  private final MediaStream<?> mediaStream;
  private final IdentificationService idService;

  public UnknownStreamException(MediaStream<?> mediaStream, IdentificationService idService) {
    super("Unable to identify " + mediaStream + " with " + idService);

    if(mediaStream == null) {
      throw new IllegalArgumentException("mediaStream cannot be null");
    }
    if(idService == null) {
      throw new IllegalArgumentException("idService cannot be null");
    }

    this.mediaStream = mediaStream;
    this.idService = idService;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mediaStream, idService);
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
    if(!mediaStream.equals(other.mediaStream)) {
      return false;
    }

    return true;
  }
}
