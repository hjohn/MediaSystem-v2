package hs.mediasystem.plugin.home;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.util.ImageURI;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

public interface MenuOption {
  String getParentTitle();
  String getTitle();
  String getSubtitle();
  String getSequence();
  Optional<Instant> getRecommendationLastTimeWatched();
  Supplier<? extends Presentation> getPresentationSupplier();
  Optional<ImageURI> getImage();
  Optional<ImageURI> getBackdrop();
  double getWatchedFraction();
}
