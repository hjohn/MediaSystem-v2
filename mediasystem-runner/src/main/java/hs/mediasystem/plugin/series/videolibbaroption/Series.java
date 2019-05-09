package hs.mediasystem.plugin.series.videolibbaroption;

import hs.mediasystem.plugin.library.scene.view.PresentationLoader;
import hs.mediasystem.plugin.library.scene.view.SerieCollectionPresentation;
import hs.mediasystem.plugin.videolibbar.rootmenu.VideoLibraryMenuPlugin;

import javafx.event.ActionEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Series implements VideoLibraryMenuPlugin.OptionPlugin {
  @Inject private SerieCollectionPresentation.Factory serieCollectionPresentationFactory;

  @Override
  public void onSelect(ActionEvent e) {
    PresentationLoader.navigate(e, () -> serieCollectionPresentationFactory.create(null));  // FIXME kill
  }
}
