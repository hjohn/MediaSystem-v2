package hs.mediasystem.runner.root;

import hs.mediasystem.presentation.ParentPresentation;
import hs.mediasystem.runner.util.debug.DebugFX;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import javax.inject.Singleton;

@Singleton
public class RootPresentation extends ParentPresentation {
  public final BooleanProperty clockVisible = new SimpleBooleanProperty(true);
  public final BooleanProperty hiddenItemsVisible = new SimpleBooleanProperty(false);
  public final BooleanProperty fpsGraphVisible = new SimpleBooleanProperty(false);

  public void toggleDebug() {
    boolean enabled = !DebugFX.getEnabled();

    System.out.println("Setting continuous debug checking to " + (enabled ? "ENABLED" : "DISABLED"));

    DebugFX.setEnabled(enabled);
  }

  public void debugOnce() {
    System.out.println("Checking references once...");

    DebugFX.checkReferences();
  }
}
