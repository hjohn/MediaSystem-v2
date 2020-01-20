package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.work.Collection;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.util.ImageURI;

import java.time.Instant;
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
  public Supplier<? extends Presentation> getPresentationSupplier() {
    return presentationSupplier;
  }

  @Override
  public String getParentTitle() {
    return null;
  }

  @Override
  public String getTitle() {
    return collection.getTitle();
  }

  @Override
  public String getSubtitle() {
    return null;
  }

  @Override
  public String getSequence() {
    return null;
  }

  @Override
  public Optional<Instant> getRecommendationLastTimeWatched() {
    return Optional.empty();
  }

  @Override
  public Optional<ImageURI> getImage() {
    return collection.getImage();
  }

  @Override
  public Optional<ImageURI> getBackdrop() {
    return collection.getBackdrop();
  }

  @Override
  public double getWatchedFraction() {
    return 0;
  }
}