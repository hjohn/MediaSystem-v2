package hs.mediasystem.plugin.library.scene.view.z;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ParentPresentation implements Presentation {
  private final ObjectProperty<Presentation> childPresentation = new SimpleObjectProperty<>();

  public ObjectProperty<Presentation> childPresentationProperty() {
    return childPresentation;
  }
}
