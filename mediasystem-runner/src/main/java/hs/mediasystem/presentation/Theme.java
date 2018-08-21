package hs.mediasystem.presentation;

public interface Theme {
  Class<? extends Presentation> findParent(Class<? extends Presentation> cls);
  <P extends ParentPresentation, C extends Presentation> Placer<P, C> findPlacer(P parentPresentation, C childPresentation);
  <P extends Presentation> P createPresentation(Class<P> presentationClass);
}
