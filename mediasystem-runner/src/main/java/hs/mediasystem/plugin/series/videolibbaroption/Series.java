package hs.mediasystem.plugin.series.videolibbaroption;

import hs.mediasystem.plugin.library.scene.view.SerieCollectionPresentation;
import hs.mediasystem.plugin.videolibbar.rootmenu.VideoLibraryMenuPlugin;
import hs.mediasystem.runner.SceneNavigator;

import javafx.event.ActionEvent;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class Series implements VideoLibraryMenuPlugin.OptionPlugin {
  @Inject private SceneNavigator navigator;
  @Inject private Provider<SerieCollectionPresentation> serieCollectionPresentationProvider;

  @Override
  public void onSelect(ActionEvent e) {
    navigator.navigateTo(serieCollectionPresentationProvider.get());
    e.consume();
  }
}
