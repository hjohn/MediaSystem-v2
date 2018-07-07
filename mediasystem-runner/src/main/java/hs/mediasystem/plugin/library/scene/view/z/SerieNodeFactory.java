package hs.mediasystem.plugin.library.scene.view.z;

import javafx.scene.Node;
import javafx.scene.control.Label;

import javax.inject.Singleton;

@Singleton
public class SerieNodeFactory {

  public Node create(SeriePresentation presentation) {
    return new Label("Serie stuff");
  }
}
