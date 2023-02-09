package hs.mediasystem.plugin.library.scene.base;

import hs.mediasystem.presentation.ParentPresentation;
import hs.mediasystem.util.image.ImageHandle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.inject.Named;

@Named
public class LibraryPresentation extends ParentPresentation {
  public final ObjectProperty<ImageHandle> backdrop = new SimpleObjectProperty<>();
}
