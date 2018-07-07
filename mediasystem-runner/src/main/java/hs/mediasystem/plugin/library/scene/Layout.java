package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.plugin.library.scene.view.x.Fragment;

public interface Layout<P, C> {
  Class<?> getLocationClass();
  Fragment<C> createView(P parentPresentation);
}
