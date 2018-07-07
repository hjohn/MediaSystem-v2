package hs.mediasystem.plugin.library.scene.view.z;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class RootPresentation extends ParentPresentation {
  public final ObjectProperty<Presentation> child = new SimpleObjectProperty<>();
}
