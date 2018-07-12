package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.presentation.Presentation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ProductionDetailPresentation implements Presentation {
  public final ObjectProperty<MediaItem<?>> mediaItem = new SimpleObjectProperty<>();

  public ProductionDetailPresentation set(MediaItem<?> mediaItem) {
    this.mediaItem.set(mediaItem);

    return this;
  }
}
