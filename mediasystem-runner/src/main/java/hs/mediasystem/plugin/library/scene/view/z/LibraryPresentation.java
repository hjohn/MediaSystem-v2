package hs.mediasystem.plugin.library.scene.view.z;

import hs.mediasystem.util.ImageHandle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class LibraryPresentation implements Presentation {
  public final ObjectProperty<ImageHandle> backdrop = new SimpleObjectProperty<>();
}
