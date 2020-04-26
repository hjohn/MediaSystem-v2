package hs.mediasystem.util.javafx;

import hs.mediasystem.util.Cache;
import hs.mediasystem.util.ImageHandle;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class ImageCache {
  private static final Logger LOGGER = Logger.getLogger(ImageCache.class.getName());
  private static final ReferenceQueue<? super CompletableFuture<Image>> REFERENCE_QUEUE = new ReferenceQueue<>();
  private static final NavigableMap<String, ImageFutureWeakReference> CACHE = new TreeMap<>();
  private static final Cache REF_CACHE = new Cache(500 * 1024 * 1024, 300 * 1000);

  public static Image getImageUptoMaxSize(ImageHandle handle, int w, int h) {
    String key = createKey(handle.getKey(), w, h, true);

    synchronized(CACHE) {
      return getFromRef(CACHE.get(key));
    }
  }

  public static Image getClosestImage(ImageHandle handle, int w, int h) {
    String key = createKey(handle.getKey(), w, h, true);

    synchronized(CACHE) {
      NavigableMap<String, ImageFutureWeakReference> subMap = CACHE.subMap(handle.getKey(), true, handle.getKey() + "$", false);
      Entry<String, ImageFutureWeakReference> entry = Optional.ofNullable(subMap.ceilingEntry(key)).orElseGet(() -> subMap.floorEntry(key));

      if(entry == null) {
        return null;
      }

      return getFromRef(entry.getValue());
    }
  }

  private static Image getFromRef(WeakReference<CompletableFuture<Image>> ref) {
    if(ref == null) {
      return null;
    }

    CompletableFuture<Image> future = ref.get();

    if(future == null) {
      return null;
    }

    try {
      return future.getNow(null);
    }
    catch(Exception e) {
      return null;
    }
  }

  public static int[] computeImageSize(int sourceWidth, int sourceHeight, int finalWidth, int finalHeight) {
    float scale = Math.min((float) finalWidth / sourceWidth, (float) finalHeight / sourceHeight);

    return new int[] {
      Math.max(1, Math.round(sourceWidth * scale)),
      Math.max(1, Math.round(sourceHeight * scale))
    };
  }

  public static CompletableFuture<Image> loadImageAsync(ImageHandle imageHandle, int maxWidth, int maxHeight, Executor executor) {
    cleanReferenceQueue();

    String key = maxWidth == 0 || maxHeight == 0 ? imageHandle.getKey() : createKey(imageHandle.getKey(), maxWidth, maxHeight, true);

    synchronized(CACHE) {
      ImageFutureWeakReference futureImageRef = CACHE.get(key);

      if(futureImageRef != null) {
        CompletableFuture<Image> futureImage = futureImageRef.get();

        if(futureImage != null && !futureImage.isCancelled()) {
          return futureImage;
        }
      }

      CancellableCompletableFuture<Image> futureImage = new CancellableCompletableFuture<>(cf -> {
        try {
          Image image = loadImage(imageHandle, maxWidth, maxHeight);

          cf.complete(image);

          // Store futureImage, not image, as otherwise the future might get GC'd even though the image wasn't
          REF_CACHE.add(key, cf, (long)image.getWidth() * (long)image.getHeight() * 4);
        }
        catch(InterruptedException e) {
          cf.cancel(true);
        }
        catch(Exception e) {
          cf.completeExceptionally(e);
        }
      }, executor);

      CACHE.put(key, new ImageFutureWeakReference(key, futureImage, REFERENCE_QUEUE));  // Must store it in the cache right away, otherwise 2nd invocation for same image would not find the one in progress

      return futureImage;
    }
  }

  public static CompletableFuture<Image> loadImageAsync(ImageHandle imageHandle, Executor executor) {
    return loadImageAsync(imageHandle, 0, 0, executor);
  }

  public static Image loadImageSync(ImageHandle imageHandle, int maxWidth, int maxHeight) throws IOException, InterruptedException {
    try {
      return loadImageAsync(imageHandle, maxWidth, maxHeight, Runnable::run).get();
    }
    catch(ExecutionException e) {
      throw new IOException("Error loading image: " + imageHandle, e);
    }
  }

  private static Image loadImage(ImageHandle imageHandle, int maxWidth, int maxHeight) throws IOException, InterruptedException {
    byte[] data = imageHandle.getImageData();

    if(Thread.interrupted()) {
      throw new InterruptedException("Image loading interrupted after loading data: " + imageHandle);
    }

    if(maxWidth == 0 || maxHeight == 0) {
      return new Image(new ByteArrayInputStream(data));
    }

    Dimension size = determineSize(data);

    if(Thread.interrupted()) {
      throw new InterruptedException("Image loading interrupted after determining size: " + imageHandle);
    }

    if(size.width <= maxWidth && size.height <= maxHeight) {
      return new Image(new ByteArrayInputStream(data));
    }

    int[] s = computeImageSize(size.width, size.height, maxWidth, maxHeight);

    return new Image(new ByteArrayInputStream(data), s[0], s[1], false, true);
  }

  private static Dimension determineSize(byte[] data) throws IOException {
    try(ImageInputStream is = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
      Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(is);

      if(imageReaders.hasNext()) {
        ImageReader imageReader = imageReaders.next();

        imageReader.setInput(is);

        int w = imageReader.getWidth(imageReader.getMinIndex());
        int h = imageReader.getHeight(imageReader.getMinIndex());

        imageReader.dispose();

        return new Dimension(w, h);
      }

      throw new IOException("No reader for given image data");
    }
  }

  private static void cleanReferenceQueue() {
    int size;
    int counter = 0;

    synchronized(CACHE) {
      size = CACHE.size();

      for(;;) {
        ImageFutureWeakReference ref = (ImageFutureWeakReference)REFERENCE_QUEUE.poll();

        if(ref == null) {
          break;
        }

        CACHE.remove(ref.getKey());

        counter++;
      }
    }

    if(counter > 0) {
      LOGGER.fine("Removed " + counter + "/" + size + " images");
    }
  }

  private static String createKey(String baseKey, double w, double h, boolean keepAspect) {
    if(w >= 100000 || h >= 100000) {
      throw new IllegalStateException("Unsupported size for image: " + w + "x" + h);
    }

    // Key will sort in such way that same images with keepAspect true will sort in order of small to large size
    return String.format("%s#%s#%09.2fx%09.2f", baseKey, keepAspect ? "T" : "F", w, h);
  }

  public static void expunge(ImageHandle handle) {
    if(handle != null) {
      String keyToRemove = handle.getKey();

      synchronized(CACHE) {
        for(Iterator<Map.Entry<String, ImageFutureWeakReference>> iterator = CACHE.tailMap(keyToRemove).entrySet().iterator(); iterator.hasNext();) {
          Map.Entry<String, ImageFutureWeakReference> entry = iterator.next();

          if(!entry.getKey().startsWith(keyToRemove)) {
            break;
          }

          iterator.remove();
        }
      }
    }
  }

  private static class ImageFutureWeakReference extends WeakReference<CompletableFuture<Image>> {
    private final String key;

    public ImageFutureWeakReference(String key, CompletableFuture<Image> referent, ReferenceQueue<? super CompletableFuture<Image>> q) {
      super(referent, q);

      this.key = key;
    }

    public String getKey() {
      return key;
    }
  }

  /**
   * This subclass is specifically created to allow for cancelling of upstream
   * futures if all downstream futures were cancelled (this has some limitations
   * and won't work when sharing the future and allowing arbitrary downstream
   * operations -- which is why this is not a generic useable class).<p>
   *
   * Since CompletableFuture's themselves can not interrupt their worker threads
   * (because the future does not know which thread), a FutureTask is used
   * internally (for the first CompletableFuture only), which can be interrupted.<p>
   *
   * Use case: if the same image is already being loaded, a 2nd caller should just
   * wait until that one is finished.  If the first caller is cancelled, but not
   * the 2nd, it should still complete.  If both are cancelled the loading can
   * be cancelled.
   */
  private static class CancellableCompletableFuture<T> extends CompletableFuture<T> {
    private final Set<CompletableFuture<?>> notCancelledDependents = Collections.synchronizedSet(new HashSet<>());
    private final CancellableCompletableFuture<?> parent;
    private final FutureTask<?> associatedTask;

    public CancellableCompletableFuture(Consumer<CompletableFuture<T>> function, Executor executor) {
      this.parent = null;
      this.associatedTask = new FutureTask<>(() -> function.accept(this), null);

      executor.execute(associatedTask);
    }

    private CancellableCompletableFuture(CancellableCompletableFuture<?> parent) {
      this.parent = parent;
      this.associatedTask = null;
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
      CancellableCompletableFuture<U> cf = new CancellableCompletableFuture<>(this);

      notCancelledDependents.add(cf);

      return cf;
    }

    private void cancel(CompletableFuture<?> child) {
      notCancelledDependents.remove(child);

      if(notCancelledDependents.isEmpty()) {
        if(associatedTask != null) {
          associatedTask.cancel(true);
        }
        else {
          cancel(true);
        }

        if(parent != null) {
          parent.cancel(this);
        }
      }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      super.cancel(mayInterruptIfRunning);

      if(parent != null) {
        parent.cancel(this);
      }

      return true;
    }
  }
}
