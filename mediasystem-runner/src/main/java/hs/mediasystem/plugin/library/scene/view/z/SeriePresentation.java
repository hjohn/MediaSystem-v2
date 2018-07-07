package hs.mediasystem.plugin.library.scene.view.z;

import hs.mediasystem.ext.basicmediatypes.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class SeriePresentation implements Presentation {
  public enum State {
    SERIE, LIST, EPISODE
  }

  public final ObjectProperty<Serie> serie = new SimpleObjectProperty<>();
  public final ObjectProperty<Episode> selectedEpisode = new SimpleObjectProperty<>();
  public final ObjectProperty<State> state = new SimpleObjectProperty<>();
}
