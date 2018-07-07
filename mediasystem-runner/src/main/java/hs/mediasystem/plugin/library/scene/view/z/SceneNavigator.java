package hs.mediasystem.plugin.library.scene.view.z;

import hs.ddif.core.Injector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SceneNavigator {
  private final RootPresentation rootPresentation = new RootPresentation();
  private final BasicTheme theme = new BasicTheme();

  @Inject private Injector injector;

  public void navigateTo(Presentation presentation) {
    if(presentation == null || presentation instanceof RootPresentation) {
      throw new IllegalStateException();
    }

    // Build a list of presentations:
    List<Presentation> presentations = new ArrayList<>();

    for(Presentation p = presentation;;) {
      presentations.add(p);

      Class<? extends Presentation> parent = theme.findParent(p.getClass());

      if(parent == null) {
        throw new IllegalStateException();
      }
      if(parent.equals(RootPresentation.class)) {
        break;
      }

      ParentPresentation instance = (ParentPresentation)injector.getInstance(parent);

      instance.childPresentationProperty().set(presentation);
      p = instance;
    }

    Collections.reverse(presentations);

    // Find common parent in root presentation, and replace the stack at that level:
    ParentPresentation parent = rootPresentation;

    for(Presentation p : presentations) {
      Presentation child = parent.childPresentationProperty().get();

      if(!child.getClass().equals(p.getClass())) {
        parent.childPresentationProperty().set(p);
        break;
      }

      parent = (ParentPresentation)child;
    }
  }
}
