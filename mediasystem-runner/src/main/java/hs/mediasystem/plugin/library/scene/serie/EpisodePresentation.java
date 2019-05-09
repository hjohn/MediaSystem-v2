package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.presentation.Presentation;

public class EpisodePresentation implements Presentation {
  private final ProductionPresentation presentation;

  public EpisodePresentation(ProductionPresentation presentation) {
    this.presentation = presentation;
  }

  public void previous() {
    int index = presentation.episodeItems.indexOf(presentation.episodeItem.get());

    if(index > 0) {
      presentation.episodeItem.set(presentation.episodeItems.get(index - 1));
    }
  }

  public void next() {
    int index = presentation.episodeItems.indexOf(presentation.episodeItem.get());

    if(index < presentation.episodeItems.size() - 1) {
      presentation.episodeItem.set(presentation.episodeItems.get(index + 1));
    }
  }
}