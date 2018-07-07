package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.runner.LocationPresentation;
import hs.mediasystem.util.ImageHandle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class EntityPresentation extends LocationPresentation<LibraryLocation> {
  public final ObjectProperty<ImageHandle> backdrop = new SimpleObjectProperty<>();

  // TODO remove
  private byte[] x = new byte[1024 * 1024 * 10];

  @Override
  protected void finalize() throws Throwable {
    System.out.println("<-- " + getClass() + " finalized");

    super.finalize();
  }
}
