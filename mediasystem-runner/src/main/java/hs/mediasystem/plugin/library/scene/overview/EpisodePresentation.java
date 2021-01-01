package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.ui.api.domain.Work;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

public class EpisodePresentation implements Presentation {
  private final ReadOnlyObjectProperty<List<Work>> episodeItems;
  private final ObjectProperty<Work> selectedChild;

  public EpisodePresentation(ReadOnlyObjectProperty<List<Work>> episodeItems, ObjectProperty<Work> selectedChild) {
    this.episodeItems = episodeItems;
    this.selectedChild = selectedChild;
  }

  public void previous() {
    List<Work> episodes = episodeItems.get();
    int index = episodes.indexOf(selectedChild.getValue());

    if(index > 0) {
      selectedChild.setValue(episodes.get(index - 1));
    }
  }

  public void next() {
    List<Work> episodes = episodeItems.get();
    int index = episodes.indexOf(selectedChild.getValue());

    if(index < episodes.size() - 1) {
      selectedChild.setValue(episodes.get(index + 1));
    }
  }
}