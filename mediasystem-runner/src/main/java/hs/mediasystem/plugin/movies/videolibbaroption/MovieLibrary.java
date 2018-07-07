package hs.mediasystem.plugin.movies.videolibbaroption;

import hs.mediasystem.plugin.library.scene.view.MovieCollectionLocation;
import hs.mediasystem.plugin.videolibbar.rootmenu.VideoLibraryMenuPlugin;
import hs.mediasystem.runner.SceneNavigator;

import javafx.event.ActionEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MovieLibrary implements VideoLibraryMenuPlugin.OptionPlugin {
  @Inject private SceneNavigator navigator;

  @Override
  public void onSelect(ActionEvent e) {
    navigator.go(new MovieCollectionLocation());
  }

}
