package hs.mediasystem.runner.util;

import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.ImageURIHandler;
import hs.mediasystem.util.javafx.ImageCache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
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
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Supports URL's of format:
 *
 *    multi:[optional parameters]:uri1,uri2
 */
@Singleton
public class MultiImageURIHandler implements ImageURIHandler {
  private static final Pattern SPLIT_PATTERN = Pattern.compile(",");
  private static final Pattern SEMI_COLON_PATTERN = Pattern.compile(";");
  private static final Pattern MAIN_PATTERN = Pattern.compile(":");
  private static final Pattern POSITIONAL_PATTERN = Pattern.compile("([0-9]+,[0-9]+)(;[-0-9]+,[-0-9]+,[0-9]+,[0-9]+)+");

  @Inject private Provider<ImageHandleFactory> factoryProvider;

  @Override
  public ImageHandle handle(ImageURI uri) {
    if(uri.getUri().startsWith("multi:")) {
      ImageHandleFactory factory = factoryProvider.get();
      String[] parts = MAIN_PATTERN.split(uri.getUri(), 3);
      List<ImageHandle> handles = SPLIT_PATTERN.splitAsStream(parts[2])
        .map(p -> new ImageURI(p, null)).map(factory::fromURI).collect(Collectors.toList());

      if(parts[1].equals("landscape")) {
        return new MultiImageHandle(uri, handles, 900, 450, c -> {
          return List.of(
            new Rectangle2D(0, 0, 300, 450),
            new Rectangle2D(300, 0, 300, 450),
            new Rectangle2D(600, 0, 300, 450)
          );
        });
      }

      Matcher matcher = POSITIONAL_PATTERN.matcher(parts[1]);

      if(matcher.matches()) {
        String[] positions = SEMI_COLON_PATTERN.split(parts[1]);
        String[] sizes = SPLIT_PATTERN.split(positions[0]);
        List<Rectangle2D> rectangles = new ArrayList<>();

        for(int i = 1; i < positions.length; i++) {
          String[] numbers = SPLIT_PATTERN.split(positions[i]);

          rectangles.add(new Rectangle2D(
            Integer.parseInt(numbers[0]),
            Integer.parseInt(numbers[1]),
            Integer.parseInt(numbers[2]),
            Integer.parseInt(numbers[3])
          ));
        }

        return new MultiImageHandle(uri, handles, Integer.parseInt(sizes[0]), Integer.parseInt(sizes[1]), c -> rectangles);
      }

      return new MultiImageHandle(uri, handles, 600, 900, c -> {
        int width = 600;
        int height = 900;

        if(c <= 3) {
          return List.of(
            new Rectangle2D(0, 0, width, height),
            new Rectangle2D(width / 10 * 6, height / 10, width / 10 * 3, height / 10 * 3),
            new Rectangle2D(width / 10 * 6, height / 10 * 6, width / 10 * 3, height / 10 * 3)
          );
        }

        return List.of(
          new Rectangle2D(0, 0, width / 2, height / 2),
          new Rectangle2D(width / 2, 0, width / 2, height / 2),
          new Rectangle2D(0, height / 2, width / 2, height / 2),
          new Rectangle2D(width / 2, height / 2, width / 2, height / 2)
        );
      });
   }

    return null;
  }

  interface Positioner {
    List<Rectangle2D> positionsFor(int imageCount);
  }

  class MultiImageHandle implements ImageHandle {
    private final ImageURI uri;
    private final List<ImageHandle> handles;
    private final int width;
    private final int height;
    private final Positioner positioner;

    public MultiImageHandle(ImageURI uri, List<ImageHandle> handles, int width, int height, Positioner positioner) {
      this.uri = uri;
      this.handles = handles;
      this.width = width;
      this.height = height;
      this.positioner = positioner;
    }

    @Override
    public byte[] getImageData() {
      Canvas c = new Canvas(width, height);
      Scene scene = new Scene(new StackPane(c));
      List<Rectangle2D> locations = positioner.positionsFor(handles.size());

      c.getGraphicsContext2D().setFill(Color.BLACK);
      c.getGraphicsContext2D().fillRect(0, 0, width, height);

      for(int i = 0; i < handles.size(); i++) {
        if(i >= locations.size()) {
          break;
        }

        ImageHandle handle = handles.get(i);
        Rectangle2D location = locations.get(i);
        Image image = ImageCache.loadImageUptoMaxSize(handle, (int)location.getWidth(), (int)location.getHeight());
        double areaWidth = location.getWidth();
        double areaHeight = location.getHeight();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double fitWidth = areaWidth;
        double fitHeight = areaHeight;
        double w = Math.round(imageWidth * areaHeight / imageHeight);

        if(w <= areaWidth) {
          fitWidth = w;
        }
        else {
          fitHeight = Math.round(imageHeight * areaWidth / imageWidth);
        }

        double x = location.getMinX() + (areaWidth - fitWidth) / 2;
        double y = location.getMinY() + (areaHeight - fitHeight) / 2;

        c.getGraphicsContext2D().drawImage(image, x, y, fitWidth, fitHeight);
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
    public int hashCode() {
      return Objects.hash(uri);
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }
      if(obj == null || getClass() != obj.getClass()) {
        return false;
      }

      MultiImageHandle other = (MultiImageHandle)obj;

      if(!uri.equals(other.uri)) {
        return false;
      }

      return true;
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
