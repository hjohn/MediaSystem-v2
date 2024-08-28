package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.work.Collection;
import hs.mediasystem.plugin.home.HomePresentation.OptionsPresentation;
import hs.mediasystem.ui.api.CollectionClient;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.image.ImageHandleFactory;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;
import javax.inject.Singleton;

public final class CollectionsPresentation implements OptionsPresentation {
  public final ObjectProperty<Collection> selectedItem = new SimpleObjectProperty<>();
  public final List<Collection> collections;

  private final ImageHandleFactory imageHandleFactory;

  @Singleton
  public static class Factory {
    @Inject private CollectionClient collectionClient;
    @Inject private ImageHandleFactory imageHandleFactory;

    public CollectionsPresentation create() {
      return new CollectionsPresentation(imageHandleFactory, collectionClient.findCollections());
    }
  }

  public CollectionsPresentation(ImageHandleFactory imageHandleFactory, List<Collection> collections) {
    this.imageHandleFactory = imageHandleFactory;
    this.collections = collections;
    this.selectedItem.set(collections.isEmpty() ? null : collections.getFirst());
  }

  @Override
  public ObservableValue<ImageHandle> backdropProperty() {
    return selectedItem
      .map(c -> c.backdrop().orElse(null))
      .map(imageHandleFactory::fromURI);
  }
}
