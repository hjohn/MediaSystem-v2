package hs.mediasystem.plugin.home;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.StartupPresentationProvider.Plugin;
import hs.mediasystem.util.image.ImageHandle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;
import javax.inject.Singleton;

public class HomePresentation implements Presentation {
  public sealed interface OptionsPresentation permits RecommendationsPresentation, CollectionsPresentation, NewItemsPresentation, GeneralOptionsPresentation {
    ObservableValue<ImageHandle> backdropProperty();
  }

  public final ObjectProperty<OptionsPresentation> optionsPresentation = new SimpleObjectProperty<>();
  public final ObservableValue<ImageHandle> backdrop = optionsPresentation.flatMap(OptionsPresentation::backdropProperty);

  @Singleton
  public static class Factory implements Plugin {
    @Inject private RecommendationsPresentation.Factory recommendationsPresentationFactory;

    @Override
    public HomePresentation create() {
      return new HomePresentation(recommendationsPresentationFactory.create());
    }
  }

  public HomePresentation(OptionsPresentation initial) {
    optionsPresentation.set(initial);
  }
}
