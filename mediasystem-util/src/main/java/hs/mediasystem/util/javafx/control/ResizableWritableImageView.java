package hs.mediasystem.util.javafx.control;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ResizableWritableImageView extends ImageView {
  private final DoubleProperty width = new SimpleDoubleProperty();
  public final double getWidth() { return width.get(); }
  public final void setWidth(double width) { this.width.set(width); }
  public final DoubleProperty widthProperty() { return width; }

  private final DoubleProperty height = new SimpleDoubleProperty();
  public final double getHeight() { return height.get(); }
  public final void setHeight(double height) { this.height.set(height); }
  public final DoubleProperty heightProperty() { return height; }

  private final DelegatingPixelWriter pixelWriter = new DelegatingPixelWriter();

  public ResizableWritableImageView(double w, double h) {
    width.addListener(new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        updateImageSize();
      }
    });

    height.addListener(new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        updateImageSize();
      }
    });

    width.set(w);
    height.set(h);
  }

  @Override
  public void resize(double w, double h) {
    setWidth(w);
    setHeight(h);
  }

  public PixelWriter getPixelWriter() {
    return pixelWriter;
  }

  public void clear() {
    updateImageSize();
    System.out.println("CLEARED CANVAS!");
  }

  private void updateImageSize() {
    int w = (int)getWidth();
    int h = (int)getHeight();

    if(w < 1) {
      w = 1;
    }
    if(h < 1) {
      h = 1;
    }

    WritableImage writableImage = new WritableImage(w, h);

    setImage(writableImage);

    pixelWriter.setPixelWriter(writableImage.getPixelWriter());
  }

  private static class DelegatingPixelWriter implements PixelWriter {
    private PixelWriter pixelWriter;

    void setPixelWriter(PixelWriter pixelWriter) {
      this.pixelWriter = pixelWriter;
    }

    @Override
    public PixelFormat<?> getPixelFormat() {
      return pixelWriter.getPixelFormat();
    }

    @Override
    public void setArgb(int x, int y, int argb) {
      pixelWriter.setArgb(x, y, argb);
    }

    @Override
    public void setColor(int x, int y, Color c) {
      pixelWriter.setColor(x, y, c);
    }

    @Override
    public <T extends Buffer> void setPixels(int x, int y, int w, int h, PixelFormat<T> pixelformat, T buffer, int scanlineStride) {
      pixelWriter.setPixels(x, y, w, h, pixelformat, buffer, scanlineStride);
    }

    @Override
    public void setPixels(int x, int y, int w, int h, PixelFormat<ByteBuffer> pixelformat, byte[] buffer, int offset, int scanlineStride) {
      pixelWriter.setPixels(x, y, w, h, pixelformat, buffer, offset, scanlineStride);
    }

    @Override
    public void setPixels(int x, int y, int w, int h, PixelFormat<IntBuffer> pixelformat, int[] buffer, int offset, int scanlineStride) {
      pixelWriter.setPixels(x, y, w, h, pixelformat, buffer, offset, scanlineStride);
    }

    @Override
    public void setPixels(int dstx, int dsty, int w, int h, PixelReader reader, int srcx, int srcy) {
      pixelWriter.setPixels(dstx, dsty, w, h, reader, srcx, srcy);
    }
  }
}
