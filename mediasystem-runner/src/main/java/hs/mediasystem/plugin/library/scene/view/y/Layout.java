package hs.mediasystem.plugin.library.scene.view.y;

public interface Layout<L, V extends View> {
  public View create(V view, L location);
  public Class<L> getLocationClass();
}
