package hs.mediasystem.plugin.home;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.StartupPresentationProvider.Plugin;
import hs.mediasystem.util.domain.Tuple;
import hs.mediasystem.util.domain.Tuple.Tuple2;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.inject.Singleton;

public class HomePresentation implements Presentation {
  public final ObjectProperty<Tuple2<String, Integer>> selectedItem = new SimpleObjectProperty<>(Tuple.of("Home", 0));

  @Singleton
  public static class Factory implements Plugin {
    @Override
    public HomePresentation create() {
      return new HomePresentation();
    }
  }
}
