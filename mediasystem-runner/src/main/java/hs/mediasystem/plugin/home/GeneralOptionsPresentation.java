package hs.mediasystem.plugin.home;

import hs.mediasystem.plugin.home.HomePresentation.OptionsPresentation;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.image.ResourceImageHandle;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

public final class GeneralOptionsPresentation implements OptionsPresentation {

  @Override
  public ObservableValue<ImageHandle> backdropProperty() {
    return new SimpleObjectProperty<>(new ResourceImageHandle(HomeScreenNodeFactory.class, "options-backdrop.jpg"));
  }
}
