package hs.mediasystem.plugin.home;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.db.Recommendation;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.ImageURI;

import java.util.Optional;
import java.util.function.Supplier;

public class RecommendationMenuOptionAdapter implements MenuOption {
  private final Recommendation recommendation;
  private final Supplier<? extends Presentation> presentationSupplier;

  public RecommendationMenuOptionAdapter(Recommendation recommendation, Supplier<? extends Presentation> presentationSupplier) {
    this.recommendation = recommendation;
    this.presentationSupplier = presentationSupplier;
  }

  @Override
  public MediaDescriptor getMediaDescriptor() {
    return recommendation.getMediaDescriptor();
  }

  private Details getDetails() {
    return recommendation.getParent().orElse(getMediaDescriptor()).getDetails();
  }

  @Override
  public Supplier<? extends Presentation> getPresentationSupplier() {
    return presentationSupplier;
  }

  @Override
  public String getTitle() {
    return getDetails().getName();
  }

  @Override
  public String getSubtitle() {
    return recommendation.getParent().isPresent() ? getMediaDescriptor().getDetails().getName() : null;
  }

  @Override
  public Optional<ImageURI> getImage() {
    if(getMediaDescriptor() instanceof Serie) {
      return Optional.of(new ImageURI("multi:800,450;0,0,800,450;517,50,233,350:"
        + getMediaDescriptor().getDetails().getBackdrop().get().getUri() + ","
        + getMediaDescriptor().getDetails().getImage().get().getUri()));
    }

    return getMediaDescriptor() instanceof Movie || getMediaDescriptor() instanceof Serie
      ? getMediaDescriptor().getDetails().getBackdrop()
      : getMediaDescriptor().getDetails().getImage().or(() -> Optional.of(recommendation.getStreamId()).map(StreamID::asInt).map(id -> new ImageURI("localdb://" + id + "/2")));
  }

  @Override
  public Optional<ImageURI> getBackdrop() {
    return getDetails().getBackdrop();
  }

  @Override
  public double getWatchedFraction() {
    if(recommendation.isWatched()) {
      return 1.0;
    }

    return recommendation.getLength().map(len -> recommendation.getPosition().toSeconds() / (double)len.toSeconds()).orElse(0.0);
  }
}