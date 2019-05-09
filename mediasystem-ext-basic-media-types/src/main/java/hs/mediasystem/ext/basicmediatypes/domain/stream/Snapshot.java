package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.util.ImageURI;

public class Snapshot {
  private final ImageURI imageUri;
  private final int frameNumber;

  public Snapshot(ImageURI imageUri, int frameNumber) {
    this.imageUri = imageUri;
    this.frameNumber = frameNumber;
  }

  public ImageURI getImageUri() {
    return imageUri;
  }

  public int getFrameNumber() {
    return frameNumber;
  }
}
