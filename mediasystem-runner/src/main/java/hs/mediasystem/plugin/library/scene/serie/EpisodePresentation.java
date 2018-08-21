package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.framework.actions.Expose;
import hs.mediasystem.presentation.Presentation;

public class EpisodePresentation implements Presentation {
  private final ProductionPresentation.Model model;

  public EpisodePresentation(ProductionPresentation.Model model) {
    this.model = model;
  }

  @Expose
  public void previous() {
    int index = model.episodeItems.indexOf(model.episodeItem.get());

    if(index > 0) {
      model.episodeItem.set(model.episodeItems.get(index - 1));
    }
  }

  @Expose
  public void next() {
    int index = model.episodeItems.indexOf(model.episodeItem.get());

    if(index < model.episodeItems.size() - 1) {
      model.episodeItem.set(model.episodeItems.get(index + 1));
    }
  }
}