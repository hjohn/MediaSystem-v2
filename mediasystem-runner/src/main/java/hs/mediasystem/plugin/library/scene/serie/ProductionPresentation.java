package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.Navigable;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;

public class ProductionPresentation implements Navigable, Presentation {
//  private static final String SYSTEM = "hs.mediasystem:UI/Library/ProductionPresentation";
//  @Inject private SettingsStore settingsStore;

  public enum State {
    OVERVIEW, LIST, EPISODE
  }

  public final ObjectProperty<MediaItem<?>> productionItem = new SimpleObjectProperty<>();
  public final ObjectProperty<State> state = new SimpleObjectProperty<>(State.OVERVIEW);
  public final ObjectProperty<MediaItem<?>> episodeItem = new SimpleObjectProperty<>();

  @Override
  public void navigateBack(Event e) {
    switch(state.get()) {
    case OVERVIEW:
      return;
    case LIST:
      state.set(State.OVERVIEW);
      break;
    case EPISODE:
      state.set(State.LIST);
      break;
    }

    e.consume();
  }

  public ProductionPresentation set(MediaItem<?> productionItem) {
    this.productionItem.set(productionItem);
    this.state.set(State.OVERVIEW);
    this.episodeItem.set(null);

//    if(productionItem.getData() instanceof Serie) {
//      String id = productionItem.getId();
//
//      if(id != null) {
//        String episode = settingsStore.getSetting(SYSTEM, id + ":episode");
//
//        Serie serie = (Serie)productionItem.getData();
//
//        serie.getSeasons().stream()
//          .flatMap(s -> s.getEpisodes().stream())
//          .filter(e -> (e.getSeasonNumber() + ":" + e.getNumber()).equals(episode))
//          .findFirst()
//          .ifPresent(e -> this.episodeItem.set(e));
//      }
//    }

    return this;
  }
}
