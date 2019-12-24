package hs.mediasystem.plugin.home;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.db.Collection;
import hs.mediasystem.util.ImageURI;

import java.util.Optional;
import java.util.function.Supplier;

public class CollectionMenuOptionAdapter implements MenuOption {
  private final Collection collection;
  private final Supplier<? extends Presentation> presentationSupplier;

  public CollectionMenuOptionAdapter(Collection collection, Supplier<? extends Presentation> presentationSupplier) {
    this.collection = collection;
    this.presentationSupplier = presentationSupplier;
  }

  @Override
  public MediaDescriptor getMediaDescriptor() {
    return null;
  }

  @Override
  public Supplier<? extends Presentation> getPresentationSupplier() {
    return presentationSupplier;
  }

  @Override
  public String getTitle() {
    return collection.getDetails().getName();
  }

  @Override
  public String getSubtitle() {
    return null;
  }

  @Override
  public Optional<ImageURI> getImage() {
    return collection.getDetails().getImage();
  }

  @Override
  public Optional<ImageURI> getBackdrop() {
    return collection.getDetails().getBackdrop();
  }

  @Override
  public double getWatchedFraction() {
    return 0;
  }
}