package hs.mediasystem.util.javafx;

import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.NamedThreadFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;

/**
 * Image property that loads the given ImageHandle in the background.<p>
 *
 * - When ImageHandle changes, Image is set to null.
 * - The background loading process will never set Image to a value that
 *   does not correspond to the current ImageHandle (when for example it
 *   was changed again before the loading completed).
 *
 * Initially, a lower resolution Image can be loaded if available in the
 * cache.  If this is the case or there was no Image in the cache, an
 * asynchronous load of a higher quality version starts.
 */
public class AsyncImageProperty3 extends SimpleObjectProperty<Image> {
  private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("AsyncImageProperty", Thread.MIN_PRIORITY, true));

  private final ObjectProperty<ImageHandle> imageHandle = new SimpleObjectProperty<>();
  public ObjectProperty<ImageHandle> imageHandleProperty() { return imageHandle; }

  private Loader loader;

  public AsyncImageProperty3(int maxWidth, int maxHeight) {
    imageHandle.addListener((observable, oldValue, value) -> {
      if(loader != null) {
        loader.cancel();
        loader = null;
      }

      if(value == null) {
        set(null);
      }
      else {
        loader = Loader.create(AsyncImageProperty3.this, value, maxWidth, maxHeight);

        if(loader != null) {
          EXECUTOR.execute(loader);
        }
      }
    });
  }

  public AsyncImageProperty3() {
    this(640, 480);
  }

  public static class Loader implements Runnable {
    private final ImageHandle imageHandle;
    private final int maxWidth;
    private final int maxHeight;
    private final WeakReference<AsyncImageProperty3> ref;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public static Loader create(AsyncImageProperty3 property, ImageHandle imageHandle, int maxWidth, int maxHeight) {
      // First, on same thread, check if image exists in cache (of any size)...
      Image image = ImageCache.getClosestImage(imageHandle, maxWidth, maxHeight);

      if(image != null) {
        property.set(image);

        if(image.getWidth() >= maxWidth - 1 || image.getHeight() >= maxHeight - 1) {
          return null;  // if Image was large enough, then no need to load anything further, donot return a Loader instance.
        }
      }

      return new Loader(property, imageHandle, maxWidth, maxHeight);
    }

    private Loader(AsyncImageProperty3 property, ImageHandle imageHandle, int maxWidth, int maxHeight) {
      this.imageHandle = imageHandle;
      this.maxHeight = maxHeight;
      this.maxWidth = maxWidth;
      this.ref = new WeakReference<>(property);
    }

    public void cancel() {
      this.cancelled.set(true);
    }

    public boolean isCancelled() {
      return ref.get() == null || cancelled.get();
    }

    @Override
    public void run() {
      if(isCancelled()) {
        return;
      }

      Image image = ImageCache.loadImageUptoMaxSize(imageHandle, maxWidth, maxHeight);

      if(image == null || isCancelled()) {
        return;
      }

      Platform.runLater(() -> {
        AsyncImageProperty3 property = ref.get();

        if(property != null && !isCancelled()) {
          property.set(image);
        }
      });
    }
  }
}
