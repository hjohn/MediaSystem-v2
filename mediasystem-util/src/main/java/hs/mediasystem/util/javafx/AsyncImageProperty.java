package hs.mediasystem.util.javafx;

import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.NamedThreadFactory;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Dimension2D;
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
public class AsyncImageProperty extends SimpleObjectProperty<Image> {
  private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(3, new NamedThreadFactory("AsyncImageProperty", Thread.MIN_PRIORITY, true));

  private final ObjectProperty<ImageHandle> imageHandle = new SimpleObjectProperty<>();
  public ObjectProperty<ImageHandle> imageHandleProperty() { return imageHandle; }

  private ScheduledFuture<?> loader;

  public AsyncImageProperty(Dimension2D maxSize, Duration settlingDelay) {
    imageHandle.addListener((observable, oldValue, value) -> {
      if(loader != null) {
        loader.cancel(true);
        loader = null;
      }

      if(value == null) {
        set(null);
      }
      else {
        if(settlingDelay.isZero()) {
          // Check if image exists in cache (of any size)...
          Image image = ImageCache.getClosestImage(value, (int)maxSize.getWidth(), (int)maxSize.getHeight());

          set(image); // image can be null, but it needs to be cleared anyway then
        }
        else {
          set(null);  // always clear if there is a settling delay
        }

        // If there was no image still or it is too small, then async load (a better) one:
        if(get() == null || get().getWidth() < maxSize.getWidth() - 1 || get().getHeight() < maxSize.getHeight() - 1) {
          loader = EXECUTOR.schedule(
            new Loader(AsyncImageProperty.this, value, maxSize),
            settlingDelay.toMillis(),
            TimeUnit.MILLISECONDS
          );
        }
      }
    });
  }

  public AsyncImageProperty(Duration settlingDelay) {
    this(new Dimension2D(640, 480), settlingDelay);
  }

  public AsyncImageProperty(int maxWidth, int maxHeight) {
    this(new Dimension2D(maxWidth, maxHeight), Duration.ZERO);
  }

  public AsyncImageProperty() {
    this(640, 480);
  }

  /**
   * Helper class to load an image in the background.  Note that this class is
   * independent of the property that created it and it won't prevent the property
   * from being garbage collected.
   */
  static class Loader implements Runnable {
    final ImageHandle imageHandle;
    final WeakReference<AsyncImageProperty> ref;
    final Dimension2D maxSize;

    Loader(AsyncImageProperty property, ImageHandle imageHandle, Dimension2D maxSize) {
      this.imageHandle = imageHandle;
      this.maxSize = maxSize;
      this.ref = new WeakReference<>(property);
    }

    @Override
    public void run() {
      if(ref.get() == null) {
        return;
      }

      Image image = ImageCache.loadImageUptoMaxSize(imageHandle, (int)maxSize.getWidth(), (int)maxSize.getHeight());

      if(Thread.interrupted() || image == null || ref.get() == null) {
        return;
      }

      Platform.runLater(() -> {
        AsyncImageProperty property = ref.get();

        if(property != null && Objects.equals(property.imageHandle.get(), imageHandle)) {
          property.set(image);
        }
      });
    }
  }
}
