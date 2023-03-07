package hs.mediasystem.runner.presentation;

import hs.mediasystem.presentation.ParentPresentation;
import hs.mediasystem.presentation.Theme;

import java.util.function.Consumer;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ViewPortFactory {
  @Inject private Theme theme;

  public ViewPort create(ParentPresentation presentation, Consumer<Node> nodeAdjuster) {
    return ViewPort.ofPresentation(theme, presentation, nodeAdjuster);
  }

  public ViewPort create(ParentPresentation presentation) {
    return create(presentation, null);
  }
}
