package hs.mediasystem.runner;

import hs.mediasystem.framework.actions.Expose;
import hs.mediasystem.presentation.ParentPresentation;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class RootPresentation extends ParentPresentation {
  public final BooleanProperty clockVisible = new SimpleBooleanProperty(true);

  @Expose
  public void toggleDebug() {
    boolean enabled = !DebugFX.getEnabled();

    System.out.println("Setting continuous debug checking to " + (enabled ? "ENABLED" : "DISABLED"));

    DebugFX.setEnabled(enabled);
  }

  @Expose
  public void debugOnce() {
    System.out.println("Checking references once...");

    DebugFX.checkReferences();
  }
}
