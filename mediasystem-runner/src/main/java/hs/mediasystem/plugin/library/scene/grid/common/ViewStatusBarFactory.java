package hs.mediasystem.plugin.library.scene.grid.common;

import hs.mediasystem.plugin.library.scene.grid.common.GridViewPresentationFactory.GridViewPresentation;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.resource.ResourceManager;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.Labels;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.scene.layout.VBox;

import javax.inject.Singleton;

@Singleton
public class ViewStatusBarFactory {
  private static final String STYLES_URL = LessLoader.compile(ViewStatusBarFactory.class, "status-bar.less");
  private static final ResourceManager RESOURCES = new ResourceManager(GridViewPresentationFactory.class);

  public VBox create(GridViewPresentation<?, ?> presentation) {
    ReadOnlyIntegerProperty totalItemCount = presentation.totalItemCount;
    ReadOnlyIntegerProperty visibleUniqueItemCount = presentation.visibleUniqueItemCount;

    StringBinding binding = Bindings.createStringBinding(
      () -> String.format(visibleUniqueItemCount.getValue().equals(totalItemCount.getValue()) ? RESOURCES.getText("status-message.unfiltered") : RESOURCES.getText("status-message.filtered"), visibleUniqueItemCount.getValue(), totalItemCount.getValue()),
      visibleUniqueItemCount,
      totalItemCount
    );

    GridPane gridPane = new GridPane();
    VBox vbox = Containers.vbox("status-bar", Labels.create("total", binding), gridPane);

    vbox.getStylesheets().add(STYLES_URL);

    gridPane.at(0, 0).add(Containers.hbox("header-with-shortcut", Labels.create("header", RESOURCES.getText("header.order")), Labels.create("remote-shortcut, red")));
    gridPane.at(0, 1).add(Labels.create("status-bar-element", presentation.sortOrder.map(so -> RESOURCES.getText("sort-order", so.resourceKey)).orElse("Unknown")));
    gridPane.at(1, 0).add(Containers.hbox("header-with-shortcut", Labels.create("header", RESOURCES.getText("header.filter")), Labels.create("remote-shortcut, green")));
    gridPane.at(1, 1).add(Labels.create("status-bar-element", presentation.filter.map(f -> RESOURCES.getText("filter", f.resourceKey)).orElse("Unknown")));

    if(presentation.availableStateFilters.size() > 1) {
      gridPane.at(2, 0).add(Containers.hbox("header-with-shortcut", Labels.create("header", RESOURCES.getText("header.stateFilter")), Labels.create("remote-shortcut, yellow")));
      gridPane.at(2, 1).add(Labels.create("status-bar-element", presentation.stateFilter.map(sf -> RESOURCES.getText("stateFilter", sf.resourceKey)).orElse("Unknown")));
    }

    BooleanBinding hidden = Bindings.size(presentation.availableGroupings).lessThan(2);

    gridPane.at(3, 0).add(Containers.hbox("header-with-shortcut", Labels.create("header", RESOURCES.getText("header.grouping"), Labels.hide(hidden)), Labels.create("remote-shortcut, blue", "", Labels.hide(hidden))));
    gridPane.at(3, 1).add(Labels.create("status-bar-element", presentation.grouping.map(g -> RESOURCES.getText("grouping", g.getClass().getSimpleName())).orElse("Unknown"), Labels.hide(hidden)));

    return vbox;
  }
}
