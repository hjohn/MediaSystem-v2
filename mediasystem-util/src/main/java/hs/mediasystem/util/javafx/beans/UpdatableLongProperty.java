package hs.mediasystem.util.javafx.beans;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;

public class UpdatableLongProperty extends SimpleLongProperty {

  public void update(final long value) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        UpdatableLongProperty.super.set(value);
      }
    });
  }
}