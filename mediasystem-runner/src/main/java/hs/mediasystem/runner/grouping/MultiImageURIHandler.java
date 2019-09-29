package hs.mediasystem.runner.grouping;

import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.ImageURIHandler;
import hs.mediasystem.util.javafx.ImageCache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class MultiImageURIHandler implements ImageURIHandler {
  private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

  @Inject private Provider<ImageHandleFactory> factoryProvider;

  @Override
  public ImageHandle handle(ImageURI uri) {
    if(uri.getUri().startsWith("multi:")) {
      return new MultiImageHandle(uri, 600, 900);
   }

    return null;
  }

  class MultiImageHandle implements ImageHandle {
    private final ImageURI uri;
    private final List<ImageHandle> handles;
    private final int width;
    private final int height;

    public MultiImageHandle(ImageURI uri, int width, int height) {
      ImageHandleFactory factory = factoryProvider.get();

      this.uri = uri;
      this.handles = SPLIT_PATTERN.splitAsStream(uri.getUri().substring(6)).map(ImageURI::new).map(factory::fromURI).collect(Collectors.toList());
      this.width = width;
      this.height = height;
    }

    @Override
    public byte[] getImageData() {
      Canvas c = new Canvas(width, height);
      Scene scene = new Scene(new StackPane(c));

      // 1 -> 1
      // 2 -> 1 big, 2 small top right
      // 3 -> 1 big, 2 small top right, 3 small bottom right
      // 4 -> spread equally
      // 5+ unsupported

      List<Rectangle2D> locations = new ArrayList<>();

      if(handles.size() <= 3) {
        locations.add(new Rectangle2D(0, 0, width, height));
        locations.add(new Rectangle2D(width / 10 * 6, height / 10, width / 10 * 3, height / 10 * 3));
        locations.add(new Rectangle2D(width / 10 * 6, height / 10 * 6, width / 10 * 3, height / 10 * 3));
      }
      else {
        locations.add(new Rectangle2D(0, 0, width / 2, height / 2));
        locations.add(new Rectangle2D(width / 2, 0, width / 2, height / 2));
        locations.add(new Rectangle2D(0, height / 2, width / 2, height / 2));
        locations.add(new Rectangle2D(width / 2, height / 2, width / 2, height / 2));
      }

      for(int i = 0; i < handles.size(); i++) {
        if(i >= 4) {
          break;
        }

        ImageHandle handle = handles.get(i);
        Rectangle2D location = locations.get(i);
        Image image = ImageCache.loadImageUptoMaxSize(handle, 640, 480);

        c.getGraphicsContext2D().drawImage(image, location.getMinX(), location.getMinY(), location.getWidth(), location.getHeight());
      }

      CompletableFuture<WritableImage> writableImageFuture = new CompletableFuture<>();
      Platform.runLater(() -> writableImageFuture.complete(c.snapshot(null, null)));

      java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);

      try {
        SwingFXUtils.fromFXImage(writableImageFuture.get(), bufferedImage);

        scene.disposePeer();
      }
      catch(InterruptedException | ExecutionException e1) {
        return null;
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      try {
        ImageIO.write(bufferedImage, "JPG", baos);
      }
      catch(IOException e) {
        return null;
      }

      return baos.toByteArray();
    }

    @Override
    public String getKey() {
      return uri.toString();
    }

    @Override
    public boolean isFastSource() {
      return true;
    }
  }
}
