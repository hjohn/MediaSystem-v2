package hs.mediasystem.plugin.movies_top100.videolibbaroption;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.view.PresentationLoader;
import hs.mediasystem.plugin.videolibbar.rootmenu.VideoLibraryMenuPlugin;

import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MovieLibrary implements VideoLibraryMenuPlugin.OptionPlugin {
  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private VideoDatabase videoDatabase;
  @Inject private MediaItem.Factory mediaItemFactory;

  @Override
  public void onSelect(ActionEvent e) {
    PresentationLoader.navigate(e, () -> factory.create(createProductionItems()));
  }

  private ObservableList<MediaItem<Production>> createProductionItems() {
    List<Production> list = videoDatabase.queryTop100();

    return FXCollections.observableArrayList(list.stream().map(p -> mediaItemFactory.create(
      p,
      null
    )).collect(Collectors.toList()));
  }
}
