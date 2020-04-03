package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.BinderProvider;
import hs.mediasystem.plugin.library.scene.MediaGridView;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.base.ContextLayout;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.Parent;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.util.javafx.ItemSelectedEvent;
import hs.mediasystem.util.javafx.Nodes;
import hs.mediasystem.util.javafx.control.AreaPane2;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.GridPaneUtil;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.gridlistviewskin.GridListViewSkin.GroupDisplayMode;
import hs.mediasystem.util.javafx.control.transition.EffectList;
import hs.mediasystem.util.javafx.control.transition.StandardTransitions;
import hs.mediasystem.util.javafx.control.transition.TransitionPane;
import hs.mediasystem.util.javafx.control.transition.effects.Fade;
import hs.mediasystem.util.javafx.control.transition.effects.Slide;
import hs.mediasystem.util.javafx.control.transition.effects.Slide.Direction;
import hs.mediasystem.util.javafx.control.transition.multi.Custom;

import java.util.List;
import java.util.Objects;

import javafx.animation.Interpolator;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javax.inject.Inject;

import org.reactfx.EventStreams;
import org.reactfx.value.Val;

public abstract class AbstractSetup<T, P extends GridViewPresentation<T>> implements NodeFactory<P> {
  protected static final String SYSTEM_PREFIX = "MediaSystem:Library:Presentation:";

  private static final ResourceManager RESOURCES = new ResourceManager(GridViewPresentation.class);

  @Inject private ContextLayout contextLayout;
  @Inject private WorkCellPresentation.Factory workCellPresentationFactory;
  @Inject private BinderProvider binderProvider;

  public enum Area {
    CENTER_TOP,
    CENTER,
    NAVIGATION,
    NAME,
    DETAILS,
    INFORMATION_PANEL,
    CONTEXT_PANEL,  // Panel that quickly fades in on the left depending on the presence of content
    PREVIEW_PANEL   // Panel that slides in from the right depending on the presence of content
  }

  public void configurePanes(AreaPane2<Area> areaPane, P presentation) {
    MediaGridView<Object> listView = new MediaGridView<>();

    listView.setOrientation(Orientation.VERTICAL);
    listView.pageByGroup.set(true);
    listView.groupDisplayMode.set(GroupDisplayMode.FOCUSED);

    listView.getStyleClass().add("glass-pane");
    listView.onItemSelected.set(e -> {
      if(!(e.getItem() instanceof Parent) || ((Parent<?>)e.getItem()).getChildren().isEmpty()) {
        onItemSelected((ItemSelectedEvent<T>)e, presentation);
      }
      else {
        presentation.contextItem.setValue(e.getItem());
      }
    });
    listView.getSelectionModel().selectedItemProperty().addListener((ov, old, current) -> {
      if(current != null) {
        Node context = contextLayout.create(current);

        if(context != null) {
          areaPane.add(Area.PREVIEW_PANEL, context);
        }
      }
    });

    areaPane.add(Area.CENTER, listView);

    listView.getProperties().put("presentation2", workCellPresentationFactory.create(listView));

    MediaGridViewCellFactory<Object> cellFactory = new MediaGridViewCellFactory<>(binderProvider);

    cellFactory.setMaxRatio(0.9);
    cellFactory.setContentBias(Orientation.VERTICAL);

    EventStreams.invalidationsOf(presentation.contextItem)
      .withDefaultEvent(null)
      .conditionOnShowing(listView)
      .observe(ci -> {
        Node contextPanel = createContextPanel(presentation);

        areaPane.clear(Area.CONTEXT_PANEL);

        if(contextPanel != null) {
          areaPane.add(Area.CONTEXT_PANEL, contextPanel);
        }
      });

    EventStreams.invalidationsOf(presentation.groups)
      .withDefaultEvent(null)
      .conditionOnShowing(listView)
      .map(x -> presentation.groups)
      .map(list -> list.isEmpty() ? null : list)
      .feedTo(listView.groups);

    Nodes.visible(listView).values().map(visible -> visible ? presentation.items : FXCollections.emptyObservableList()).feedTo(listView.itemsProperty());

    listView.setCellFactory(cellFactory);
    listView.requestFocus();

    setupStatusBar(areaPane, presentation);

    listView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> {
      if(current != null) {
        presentation.selectItem(current);
      }
    });

    EventStreams.valuesOf(presentation.selectedItem)
      .withDefaultEvent(presentation.selectedItem.getValue())
      .repeatOn(EventStreams.changesOf(listView.itemsProperty()))
      .conditionOnShowing(listView)
      .observe(item -> updateSelectedItem(listView, presentation, item));
  }

  private void updateSelectedItem(MediaGridView<Object> listView, P presentation, Object selectedItem) {
    if(Objects.equals(selectedItem, listView.getSelectionModel().getSelectedItem())) {
      return;
    }

    int selectedIndex = presentation.items.indexOf(selectedItem);

    if(selectedIndex == -1) {
      selectedIndex = 0;
    }

    listView.getSelectionModel().select(selectedIndex);
  }

  @Override
  public Node create(P presentation) {
    AreaPane2<Area> areaPane = new AreaPane2<>() {
      TransitionPane leftOverlayPanel = new TransitionPane(StandardTransitions.fade());
      TransitionPane rightOverlayPanel = new TransitionPane(new Custom(
        Duration.millis(2000),
        new EffectList(Duration.millis(500), List.of(new Slide(Interpolator.EASE_OUT, Direction.RIGHT), new Fade())),
        new EffectList(Duration.millis(500), List.of(new Slide(Interpolator.EASE_IN, Direction.RIGHT), new Fade()))
      ));
      GridPane gp = GridPaneUtil.create(new double[] {2, 21, 2, 50, 2, 21, 2}, new double[] {15, 66, 0.5, 5, 0.5, 6, 6.5, 0.5});
      HBox navigationArea = new HBox();

      {
        // gp.setGridLinesVisible(true);
        gp.setMinSize(1, 1);

        getChildren().add(gp);

        gp.at(3, 3).styleClass("navigation-area").add(navigationArea);

        // left overlay:
        gp.at(1, 1).spanning(1, 5).align(VPos.TOP).styleClass("overlay-panel").add(leftOverlayPanel);

        // right overlay:
        gp.at(5, 1).spanning(2, 5).align(VPos.TOP).styleClass("overlay-panel", "slide-in-right-panel").add(rightOverlayPanel);

        setupArea(Area.CENTER_TOP, gp, (p, n) -> p.at(3, 1).add(n));
        setupArea(Area.NAVIGATION, navigationArea);
        setupArea(Area.NAME, gp, (p, n) -> p.at(3, 5).align(HPos.CENTER).align(VPos.TOP).add(n));
        setupArea(Area.DETAILS, gp, (p, n) -> p.at(5, 1).add(n));
        setupArea(Area.CENTER, gp, (p, n) -> p.at(3, 1).spanning(1, 5).align(VPos.BOTTOM).add(n));
        setupArea(Area.CONTEXT_PANEL, leftOverlayPanel, (TransitionPane p, Node node) -> p.add(node));
        setupArea(Area.PREVIEW_PANEL, rightOverlayPanel, (TransitionPane p, Node node) -> p.add(node));
        setupArea(Area.INFORMATION_PANEL, gp, (p, n) -> p.at(1, 6).spanning(5, 1).align(VPos.BOTTOM).align(HPos.LEFT).styleClass("information-panel").add(n));
      }
    };

    areaPane.setMinSize(1, 1);
    areaPane.getStylesheets().add(LessLoader.compile(AbstractSetup.class.getResource("styles.less")).toExternalForm());

    configurePanes(areaPane, presentation);

    return areaPane;
  }

  private void setupStatusBar(AreaPane2<Area> areaPane, P presentation) {
    Val<Integer> totalItemCount = presentation.totalItemCount;
    Val<Integer> visibleUniqueItemCount = presentation.visibleUniqueItemCount;

    StringBinding binding = Bindings.createStringBinding(() -> String.format(visibleUniqueItemCount.getValue() == totalItemCount.getValue() ? RESOURCES.getText("status-message.unfiltered") : RESOURCES.getText("status-message.filtered"), visibleUniqueItemCount.getValue(), totalItemCount.getValue()), visibleUniqueItemCount, totalItemCount);
    GridPane gridPane = new GridPane();
    VBox vbox = Containers.vbox("status-bar", Labels.create("total", binding), gridPane);

    vbox.getStylesheets().add(LessLoader.compile(AbstractSetup.class.getResource("status-bar.less")).toExternalForm());

    areaPane.add(Area.INFORMATION_PANEL, vbox);

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
  }

  protected abstract void onItemSelected(ItemSelectedEvent<T> event, P presentation);

  protected Node createContextPanel(P presentation) {
    Object item = presentation.contextItem.getValue();

    return item == null ? null : contextLayout.create(item);
  }
}
