package hs.mediasystem.plugin.series.videolibbaroption;

import hs.mediasystem.plugin.library.scene.view.SerieCollectionPresentation;
import hs.mediasystem.plugin.videolibbar.rootmenu.VideoLibraryMenuPlugin;
import hs.mediasystem.runner.NavigateEvent;

import javafx.event.ActionEvent;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class Series implements VideoLibraryMenuPlugin.OptionPlugin {
  @Inject private Provider<SerieCollectionPresentation> serieCollectionPresentationProvider;

  @Override
  public void onSelect(ActionEvent e) {
    Event.fireEvent(e.getTarget(), NavigateEvent.to(serieCollectionPresentationProvider.get()));
    e.consume();
  }
}
