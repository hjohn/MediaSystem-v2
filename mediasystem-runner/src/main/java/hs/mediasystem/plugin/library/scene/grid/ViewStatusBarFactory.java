package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.Labels;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.scene.layout.VBox;

import javax.inject.Singleton;

import org.reactfx.value.Val;

@Singleton
public class ViewStatusBarFactory {
  private static final LessLoader LESS_LOADER = new LessLoader(ViewStatusBarFactory.class);
  private static final ResourceManager RESOURCES = new ResourceManager(GridViewPresentation.class);

  public VBox create(GridViewPresentation<?> presentation) {
    Val<Integer> totalItemCount = presentation.totalItemCount;
    Val<Integer> visibleUniqueItemCount = presentation.visibleUniqueItemCount;

    StringBinding binding = Bindings.createStringBinding(
      () -> String.format(visibleUniqueItemCount.getValue().equals(totalItemCount.getValue()) ? RESOURCES.getText("status-message.unfiltered") : RESOURCES.getText("status-message.filtered"), visibleUniqueItemCount.getValue(), totalItemCount.getValue()),
      visibleUniqueItemCount,
      totalItemCount
    );

    GridPane gridPane = new GridPane();
    VBox vbox = Containers.vbox("status-bar", Labels.create("total", binding), gridPane);

    vbox.getStylesheets().add(LESS_LOADER.compile("status-bar.less"));

    gridPane.at(0, 0).add(Containers.hbox("header-with-shortcut", Labels.create("header", RESOURCES.getText("header.order")), Labels.create("remote-shortcut, red")));
    gridPane.at(0, 1).add(Labels.create("status-bar-element", Val.wrap(presentation.sortOrder).map(so -> RESOURCES.getText("sort-order", so.resourceKey)).orElseConst("Unknown")));
    gridPane.at(1, 0).add(Containers.hbox("header-with-shortcut", Labels.create("header", RESOURCES.getText("header.filter")), Labels.create("remote-shortcut, green")));
    gridPane.at(1, 1).add(Labels.create("status-bar-element", Val.wrap(presentation.filter).map(f -> RESOURCES.getText("filter", f.resourceKey)).orElseConst("Unknown")));

    if(presentation.availableStateFilters.size() > 1) {
      gridPane.at(2, 0).add(Containers.hbox("header-with-shortcut", Labels.create("header", RESOURCES.getText("header.stateFilter")), Labels.create("remote-shortcut, yellow")));
      gridPane.at(2, 1).add(Labels.create("status-bar-element", Val.wrap(presentation.stateFilter).map(sf -> RESOURCES.getText("stateFilter", sf.resourceKey)).orElseConst("Unknown")));
    }

    BooleanBinding hidden = Bindings.size(presentation.availableGroupings).lessThan(2);

    gridPane.at(3, 0).add(Containers.hbox("header-with-shortcut", Labels.create("header", RESOURCES.getText("header.grouping"), Labels.hide(hidden)), Labels.create("remote-shortcut, blue", "", Labels.hide(hidden))));
    gridPane.at(3, 1).add(Labels.create("status-bar-element", Val.wrap(presentation.grouping).map(g -> RESOURCES.getText("grouping", g.getClass().getSimpleName())).orElseConst("Unknown"), Labels.hide(hidden)));

    return vbox;
  }
}
