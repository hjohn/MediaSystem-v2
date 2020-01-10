package hs.mediasystem.plugin.home;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.util.ImageURI;

import java.util.Optional;
import java.util.function.Supplier;

public interface MenuOption {
  String getTitle();
  String getSubtitle();
  Supplier<? extends Presentation> getPresentationSupplier();
  Optional<ImageURI> getImage();
  Optional<ImageURI> getBackdrop();
  double getWatchedFraction();
}
