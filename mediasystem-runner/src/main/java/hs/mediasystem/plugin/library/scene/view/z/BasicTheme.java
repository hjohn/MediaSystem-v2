package hs.mediasystem.plugin.library.scene.view.z;

public class BasicTheme {

  public Class<? extends Presentation> findParent(Class<? extends Presentation> cls) {
    if(cls == SeriePresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == LibraryPresentation.class) {
      return RootPresentation.class;
    }

    return null;
  }

  public <P extends Presentation> NodeFactory<P> findNodeFactory(Class<P> cls) {
    return null;
  }

  public Placer findPlacer(Class<? extends NodeFactory> parent, Class<? extends NodeFactory> child) {
    return null;
  }
}
