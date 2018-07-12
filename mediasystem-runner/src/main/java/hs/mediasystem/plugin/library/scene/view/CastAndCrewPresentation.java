package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.plugin.library.scene.MediaItem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class CastAndCrewPresentation extends GridViewPresentation {
  public final ObjectProperty<MediaItem<?>> mediaItem = new SimpleObjectProperty<>();

  public CastAndCrewPresentation set(MediaItem<?> mediaItem) {
    this.mediaItem.set(mediaItem);

    return this;
  }
}
