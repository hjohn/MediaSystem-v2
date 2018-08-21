package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.plugin.library.scene.MediaItem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class RecommendationsPresentation extends GridViewPresentation {
  public final ObjectProperty<MediaItem<?>> mediaItem = new SimpleObjectProperty<>();

  public RecommendationsPresentation set(MediaItem<?> mediaItem) {
    this.mediaItem.set(mediaItem);

    return this;
  }
}
