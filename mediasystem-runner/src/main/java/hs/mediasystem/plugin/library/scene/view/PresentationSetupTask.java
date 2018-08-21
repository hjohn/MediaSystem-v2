package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.presentation.Presentation;

import javafx.concurrent.Task;

public abstract class PresentationSetupTask<P extends Presentation> extends Task<Void> {
  private final Class<P> cls;

  private P presentation;

  public PresentationSetupTask(Class<P> cls) {
    this.cls = cls;
  }

  public Class<P> getPresentationClass() {
    return cls;
  }

  public void setPresentation(P presentation) {
    this.presentation = presentation;
  }

  @Override
  protected Void call() throws Exception {
    call(presentation);

    return null;
  }

  protected abstract void call(P presentation);
}
