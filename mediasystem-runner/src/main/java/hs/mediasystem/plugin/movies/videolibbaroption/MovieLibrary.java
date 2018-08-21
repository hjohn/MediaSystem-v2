package hs.mediasystem.plugin.movies.videolibbaroption;

import hs.mediasystem.plugin.videolibbar.rootmenu.VideoLibraryMenuPlugin;
import hs.mediasystem.runner.NavigateEvent;

import javafx.event.ActionEvent;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class MovieLibrary implements VideoLibraryMenuPlugin.OptionPlugin {
  @Inject private Provider<MovieCollectionPresentation> movieCollectionPresentationProvider;

  @Override
  public void onSelect(ActionEvent e) {
    Event.fireEvent(e.getTarget(), NavigateEvent.to(movieCollectionPresentationProvider.get()));
    e.consume();
  }
}
