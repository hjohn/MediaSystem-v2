package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.plugin.library.scene.MediaItem;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class SerieEpisodesPresentation extends GridViewPresentation {
  public final ObjectProperty<MediaItem<Serie>> mediaItem = new SimpleObjectProperty<>();
  public final IntegerProperty seasonNumber = new SimpleIntegerProperty();

  public SerieEpisodesPresentation set(MediaItem<Serie> mediaItem, int seasonNumber) {
    this.mediaItem.set(mediaItem);
    this.seasonNumber.set(seasonNumber);

    return this;
  }
}
