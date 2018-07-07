package hs.mediasystem.plugin.series.videolibbaroption;

import hs.mediasystem.plugin.library.scene.view.SerieCollectionLocation;
import hs.mediasystem.plugin.videolibbar.rootmenu.VideoLibraryMenuPlugin;
import hs.mediasystem.runner.SceneNavigator;

import javafx.event.ActionEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Series implements VideoLibraryMenuPlugin.OptionPlugin {
  @Inject private SceneNavigator navigator;

  @Override
  public void onSelect(ActionEvent e) {
    navigator.go(new SerieCollectionLocation());

    e.consume();
  }
}
