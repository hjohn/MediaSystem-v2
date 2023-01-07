package hs.mediasystem.util.image;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageHandleFactory {
  @Inject private List<ImageURIHandler> handlers;

  public ImageHandle fromURI(ImageURI uri) {
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }

    for(ImageURIHandler handler : handlers) {
      ImageHandle imageHandle = handler.handle(uri);

      if(imageHandle != null) {
        return imageHandle;
      }
    }

    throw new IllegalStateException("Unable to handle ImageURI: " + uri);
  }
}
