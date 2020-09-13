package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionOverviewNodeFactory;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.ProductionPresentation;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.GridPaneUtil;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Val;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = ProductionOverviewNodeFactory.class)
public class ProductionOverviewPlacer extends AbstractPlacer<LibraryPresentation, ProductionPresentation, ProductionOverviewNodeFactory> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, ProductionPresentation presentation) {
    parentPresentation.backdrop.bind(Val.constant(presentation.rootItem.getDetails().getBackdrop().map(imageHandleFactory::fromURI).orElse(null)));
  }

  @Override
  public Node place(LibraryPresentation parentPresentation, ProductionPresentation presentation) {
    Node node = super.place(parentPresentation, presentation);

    ((Pane)node).setMaxSize(10000, 10000);

    GridPane gridPane = GridPaneUtil.create(new double[] {10, 80, 10}, new double[] {25, 50, 25});

    gridPane.at(1, 1).fillWidth().fillHeight().add(node);

    return new StackPane(gridPane);
  }
}
