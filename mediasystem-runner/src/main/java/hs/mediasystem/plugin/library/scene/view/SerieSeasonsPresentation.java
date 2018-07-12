package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.plugin.library.scene.MediaItem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class SerieSeasonsPresentation extends GridViewPresentation {
  public final ObjectProperty<MediaItem<?>> mediaItem = new SimpleObjectProperty<>();

  public SerieSeasonsPresentation set(MediaItem<?> mediaItem) {
    this.mediaItem.set(mediaItem);

    return this;
  }
}
