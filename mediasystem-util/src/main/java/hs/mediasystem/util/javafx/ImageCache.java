package hs.mediasystem.util.javafx;

import hs.mediasystem.util.Cache;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.Throwables;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

  public static Image loadImage(ImageHandle handle) {
    cleanReferenceQueue();

    return loadImage(handle.getKey(), handle, () -> new Image(new ByteArrayInputStream(handle.getImageData())));
  }

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

  public static Image loadImageUptoMaxSize(ImageHandle handle, int w, int h) {
    cleanReferenceQueue();

    String key = createKey(handle.getKey(), w, h, true);

    return loadImage(key, handle, () -> {
      Image image = null;
      byte[] data = handle.getImageData();

      if(Thread.interrupted()) {
        throw new InterruptedException("Image loading interrupted after loading data: " + handle);
      }

      if(data != null) {
        Dimension size = determineSize(data);

        if(Thread.interrupted()) {
          throw new InterruptedException("Image loading interrupted after determining size: " + handle);
        }

        if(size != null) {
          if(size.width <= w && size.height <= h) {
            image = new Image(new ByteArrayInputStream(data));
          }
          else {
            int[] s = computeImageSize(size.width, size.height, w, h);

            image = new Image(new ByteArrayInputStream(data), s[0], s[1], false, true);
          }
        }
      }

      return image;
    });
  }

  public static int[] computeImageSize(int sourceWidth, int sourceHeight, int finalWidth, int finalHeight) {
    float scale = Math.min((float) finalWidth / sourceWidth, (float) finalHeight / sourceHeight);

    return new int[] {
      Math.max(1, Math.round(sourceWidth * scale)),
      Math.max(1, Math.round(sourceHeight * scale))
    };
  }

  /**
   * Loads the image matching the given key, optionally using the Image supplier if no previous
   * load was in progress.  If another load for the same image is in progress this function
   * waits for it to be completed and then returns the image.
   *
   * @param key an image key
   * @param imageSupplier an Image supplier, which will be called if needed
   * @return the image matching the given key
   */
  private static Image loadImage(String key, ImageHandle imageHandle, Callable<Image> imageSupplier) {
    CompletableFuture<Image> futureImage;
    boolean needsCompletion = false;

    /*
     * Obtain an existing Future for the Image to be loaded or create a new one:
     */

    synchronized(CACHE) {
      WeakReference<?> ref = CACHE.get(key);

      if(ref != null) {
        Object content = ref.get();

        if(content instanceof Image) {
          return (Image)content;  // The cache had an Image, return it
        }
      }

      // Normal flow, create future if needed:
      @SuppressWarnings("unchecked")
      WeakReference<CompletableFuture<Image>> futureImageRef = (WeakReference<CompletableFuture<Image>>)ref;

      futureImage = futureImageRef != null ? futureImageRef.get() : null;

      if(futureImage == null) {
        futureImage = new CompletableFuture<>();
        store(key, futureImage);
        needsCompletion = true;
      }
    }

    /*
     * Optionally trigger an image load and use it to complete the Future.  This must happen
     * outside the synchronized block as the lock would otherwise be held for the duration
     * of the image loading.
     */

    if(needsCompletion) {
      Image image = null;

      try {
        image = imageSupplier.call();

        futureImage.complete(image);
      }
      catch(Exception e) {
        LOGGER.warning("Unable to load image: " + imageHandle + ": " + Throwables.formatAsOneLine(e));

        futureImage.complete(null);
      }

      if(image == null) {
        synchronized(CACHE) {
          CACHE.remove(key);
        }
      }
    }

    /*
     * Get the final image result and return it:
     */

    try {
      Image image = futureImage.get();

      synchronized(CACHE) {
        CACHE.put(key, new ImageFutureWeakReference(key, futureImage, REFERENCE_QUEUE));

        if(image != null) {
          REF_CACHE.add(key, image, (long)image.getWidth() * (long)image.getHeight() * 4);
        }
      }

      return image;
    }
    catch(InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private static Dimension determineSize(byte[] data) {
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

      return null;
    }
    catch(IOException e) {
      return null;
    }
  }

  private static void store(String key, CompletableFuture<Image> imageFuture) {
    ImageFutureWeakReference imageRef = new ImageFutureWeakReference(key, imageFuture, REFERENCE_QUEUE);

    synchronized(CACHE) {
      CACHE.put(key, imageRef);
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
}
