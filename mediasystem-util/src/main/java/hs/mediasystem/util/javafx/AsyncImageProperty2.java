package hs.mediasystem.util.javafx;

import hs.mediasystem.util.AutoReentrantLock;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.NamedThreadFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

/**
 * Image property that loads the given ImageHandle in the background.<p>
 *
 * - When ImageHandle changes, Image is set to null and stays null for
 *   the settling duration plus the time to load a new Image.
 * - The background loading process will never set Image to a value that
 *   does not correspond to the current ImageHandle (when for example it
 *   was changed again before the loading completed).
 */

// Contains changes to remove the settling delay

public class AsyncImageProperty2 extends SimpleObjectProperty<Image> {
  private static final ThreadPoolExecutor SLOW_EXECUTOR = new ThreadPoolExecutor(2, 2, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("AsyncImageProperty[S]", Thread.NORM_PRIORITY - 2, true));
  private static final ThreadPoolExecutor FAST_EXECUTOR = new ThreadPoolExecutor(3, 3, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("AsyncImageProperty[F]", Thread.NORM_PRIORITY - 2, true));

  private static Executor JAVAFX_UPDATE_EXECUTOR = new Executor() {
    @Override
    public void execute(Runnable command) {
      Platform.runLater(command);
    }
  };

  static {
    SLOW_EXECUTOR.allowCoreThreadTimeOut(true);
    FAST_EXECUTOR.allowCoreThreadTimeOut(true);
  }

  private final ObjectProperty<ImageHandle> imageHandle = new SimpleObjectProperty<>();
  public ObjectProperty<ImageHandle> imageHandleProperty() { return imageHandle; }

  private final AutoReentrantLock backgroundLoadLock = new AutoReentrantLock();

  private final int maxWidth;
  private final int maxHeight;

  /**
   * Contains the ImageHandle to load next, or null if the settling time has not expired yet.
   */
  private volatile ImageHandle imageHandleToLoad;     // Must hold backgroundLoadLock to access
  private volatile boolean backgroundLoadInProgress;  // Must hold backgroundLoadLock to access

  public AsyncImageProperty2(int maxWidth, int maxHeight) {
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;

    imageHandle.addListener(new ChangeListener<ImageHandle>() {
      @Override
      public void changed(ObservableValue<? extends ImageHandle> observable, ImageHandle oldValue, ImageHandle value) {
        if(value == null) {
          synchronized(backgroundLoadLock) {
            set(null);
            imageHandleToLoad = null;
            backgroundLoadInProgress = false;
          }
        }
        else {
          setNewImageToLoad(new WeakReference<>(AsyncImageProperty2.this), value);

          synchronized(backgroundLoadLock) {
            if(backgroundLoadInProgress) {
              set(null);
            }
          }
        }
      }
    });
  }

  public AsyncImageProperty2() {
    this(640, 480);
  }

  private void loadImageInBackgroundIfNeeded() {
    try(AutoReentrantLock lock = backgroundLoadLock.lock()) {
      if(!backgroundLoadInProgress && imageHandleToLoad != null) {
        ImageHandle copy = imageHandleToLoad;

        backgroundLoadInProgress = true;
        imageHandleToLoad = null;
        loadImageInBackground(new WeakReference<>(this), copy, maxWidth, maxHeight);
      }
    }
  }

  /**
   * Sets a new image to load (typically called after the settling delay expired).<p>
   *
   * Declared static so all references to instances must be by weak reference.
   *
   * @param propertyRef a weak reference to an instance of this class
   * @param imageHandle the image to load
   */
  private static void setNewImageToLoad(WeakReference<AsyncImageProperty2> propertyRef, ImageHandle imageHandle) {
    AsyncImageProperty2 property = propertyRef.get();

    if(property != null) {
      try(AutoReentrantLock lock = property.backgroundLoadLock.lock()) {
        property.imageHandleToLoad = imageHandle;
        property.loadImageInBackgroundIfNeeded();
      }
    }
  }

  /**
   * Triggers the process that loads an image in the background.<p>
   *
   * Declared static so all references to instances must be by weak reference.
   *
   * @param propertyRef a weak reference to an instance of this class
   * @param imageHandle the image to load
   */
  private static void loadImageInBackground(WeakReference<AsyncImageProperty2> propertyRef, ImageHandle imageHandle, int maxWidth, int maxHeight) {
    Image cachedImage = ImageCache.getClosestImage(imageHandle, maxWidth, maxHeight);

    if(cachedImage != null) {
      AsyncImageProperty2 property = propertyRef.get();

      if(property != null) {
        try {
          if(imageHandle.equals(property.imageHandle.get())) {  // TODO deduplicate
            property.set(cachedImage);

            return;
          }
        }
        finally {
          try(AutoReentrantLock lock = property.backgroundLoadLock.lock()) {
            property.backgroundLoadInProgress = false;
            property.loadImageInBackgroundIfNeeded();
          }
        }
      }
    }

    CompletableFuture
      .supplyAsync(() -> imageHandle.isFastSource() ? FAST_EXECUTOR : SLOW_EXECUTOR, FAST_EXECUTOR)
      .thenCompose(executor -> CompletableFuture.supplyAsync(() -> getImage(propertyRef, imageHandle, maxWidth, maxHeight), executor))
      .whenCompleteAsync((image, e) -> {
        AsyncImageProperty2 property = propertyRef.get();

        if(property != null) {
          try {
            if(e == null && imageHandle.equals(property.imageHandle.get())) {
              property.set(image);
            }
          }
          finally {
            try(AutoReentrantLock lock = property.backgroundLoadLock.lock()) {
              property.backgroundLoadInProgress = false;
              property.loadImageInBackgroundIfNeeded();
            }
          }
        }

        if(e != null && !(e.getCause() instanceof CancellationException)) {
          System.out.println("[WARN] AsyncImageProperty2 - Exception while loading " + imageHandle + " in background: " + e);
        }
      }, JAVAFX_UPDATE_EXECUTOR);
  }

  /**
   * Gets an Image from the Cache.<p>
   *
   * Declared static so all references to instances must be by weak reference.
   *
   * @param propertyRef a weak reference to an instance of this class
   * @param imageHandle the image to load
   */
  private static Image getImage(WeakReference<AsyncImageProperty2> propertyRef, ImageHandle imageHandle, int maxWidth, int maxHeight) {
    final AsyncImageProperty2 asyncImageProperty = propertyRef.get();

    /*
     * Check if AsyncImageProperty still exists and its imageHandle hasn't changed:
     */

    if(asyncImageProperty != null && imageHandle.equals(asyncImageProperty.imageHandle.get())) {
      return ImageCache.loadImageUptoMaxSize(imageHandle, maxWidth, maxHeight);
    }

    throw new CancellationException();
  }
}
