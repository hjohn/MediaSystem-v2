package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.ProductionPresentation;
import hs.mediasystem.presentation.Presentation;

public class EpisodePresentation implements Presentation {
  private final ProductionPresentation presentation;

  public EpisodePresentation(ProductionPresentation presentation) {
    this.presentation = presentation;
  }

  public void previous() {
    int index = presentation.episodeItems.indexOf(presentation.episodeItem.getValue());

    if(index > 0) {
      presentation.episodeItem.setValue(presentation.episodeItems.get(index - 1));
    }
  }

  public void next() {
    int index = presentation.episodeItems.indexOf(presentation.episodeItem.getValue());

    if(index < presentation.episodeItems.size() - 1) {
      presentation.episodeItem.setValue(presentation.episodeItems.get(index + 1));
    }
  }
}