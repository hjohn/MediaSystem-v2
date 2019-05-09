package hs.mediasystem.plugin.movies.videolibbaroption;

import hs.mediasystem.plugin.library.scene.view.PresentationLoader;
import hs.mediasystem.plugin.videolibbar.rootmenu.VideoLibraryMenuPlugin;

import javafx.event.ActionEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MovieLibrary implements VideoLibraryMenuPlugin.OptionPlugin {
  @Inject private MovieCollectionPresentation.Factory movieCollectionPresentationFactory;

  @Override
  public void onSelect(ActionEvent e) {
    PresentationLoader.navigate(e, () -> movieCollectionPresentationFactory.create());
  }
}
