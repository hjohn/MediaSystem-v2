package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.framework.actions.Expose;
import hs.mediasystem.presentation.Presentation;

public class EpisodePresentation implements Presentation {
  private final EpisodesPresentation presentation;

  public EpisodePresentation(EpisodesPresentation presentation) {
    this.presentation = presentation;
  }

  @Expose
  public void previous() {
    int index = presentation.episodeItems.indexOf(presentation.episodeItem.get());

    if(index > 0) {
      presentation.episodeItem.set(presentation.episodeItems.get(index - 1));
    }
  }

  @Expose
  public void next() {
    int index = presentation.episodeItems.indexOf(presentation.episodeItem.get());

    if(index < presentation.episodeItems.size() - 1) {
      presentation.episodeItem.set(presentation.episodeItems.get(index + 1));
    }
  }
}