package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;

import java.util.Map;

public class MediaIdentification {
  private final BasicStream media;
  private final Map<StreamID, Identification> identifications;
  private final MediaDescriptor descriptor;

  public MediaIdentification(BasicStream media, Map<StreamID, Identification> identifications, MediaDescriptor descriptor) {
    if(media == null) {
      throw new IllegalArgumentException("media cannot be null");
    }
    if(identifications == null) {
      throw new IllegalArgumentException("identifications cannot be null");
    }
    if(!identifications.isEmpty() && descriptor == null) {
      throw new IllegalArgumentException("descriptor cannot be null if there was an identification");
    }

    this.media = media;
    this.identifications = identifications;
    this.descriptor = descriptor;
  }

  public Map<StreamID, Identification> getIdentifications() {
    return identifications;
  }

  public MediaDescriptor getDescriptor() {
    return descriptor;
  }

  public BasicStream getStream() {
    return media;
  }
}
