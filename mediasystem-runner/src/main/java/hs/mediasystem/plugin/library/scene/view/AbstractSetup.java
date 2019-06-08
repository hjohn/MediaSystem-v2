package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.plugin.library.scene.MediaGridView;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.util.javafx.Binds;
import hs.mediasystem.util.javafx.ItemSelectedEvent;
import hs.mediasystem.util.javafx.control.AreaPane2;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.GridPaneUtil;
import hs.mediasystem.util.javafx.control.Labels;

import java.util.Objects;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javax.inject.Inject;

import org.reactfx.EventStreams;

public abstract class AbstractSetup<T extends MediaDescriptor, P extends GridViewPresentation<T>> implements NodeFactory<P> {
  private static final ResourceManager RESOURCES = new ResourceManager(GridViewPresentation.class);

  @Inject private ContextLayout contextLayout;

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
    MediaGridView<MediaItem<T>> listView = new MediaGridView<>();

    listView.getStyleClass().add("glass-pane");
    listView.onItemSelected.set(e -> onItemSelected(e, presentation));
    listView.getSelectionModel().selectedItemProperty().addListener((ov, old, current) -> {
      if(current != null) {
        Node context = contextLayout.create(current);

        if(context != null) {
          areaPane.add(Area.PREVIEW_PANEL, context);
        }
      }
    });

    areaPane.add(Area.CENTER, listView);

    MediaGridViewCellFactory<T> cellFactory = new MediaGridViewCellFactory<>();

    cellFactory.setMaxRatio(0.9);
    cellFactory.setContentBias(Orientation.VERTICAL);

    configureCellFactory(cellFactory);

    Node contextPanel = createContextPanel(presentation);

    if(contextPanel != null) {
      areaPane.add(Area.CONTEXT_PANEL, contextPanel);
    }

    listView.setItems(presentation.items);
    listView.setCellFactory(cellFactory);
    listView.requestFocus();

    setupStatusBar(areaPane, presentation, presentation.items, presentation.inputItems);

    listView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> presentation.selectItem(current));

    EventStreams.changesOf(presentation.selectedItem)
      .conditionOnShowing(listView)
      .observe(c -> {
        updateSelectedItem(listView, presentation, c.getNewValue());
      });

    updateSelectedItem(listView, presentation, presentation.selectedItem.getValue());
  }

  private void updateSelectedItem(MediaGridView<MediaItem<T>> listView, P presentation, MediaItem<T> selectedItem) {
    if(Objects.equals(selectedItem, listView.getSelectionModel().getSelectedItem())) {
      return;
    }

    int selectedIndex = presentation.items.indexOf(selectedItem);

    if(selectedIndex == -1) {
      selectedIndex = 0;
    }

    listView.getSelectionModel().select(selectedIndex);
  }

  public Node createView(P presentation) {
    AreaPane2<Area> areaPane = new AreaPane2<>() {
      StackPane leftOverlayPanel = new StackPane();
      StackPane rightOverlayPanel = new StackPane();
      GridPane gp = GridPaneUtil.create(new double[] {2, 21, 2, 50, 2, 21, 2}, new double[] {15, 66, 0.5, 5, 0.5, 6, 6.5, 0.5});
      HBox navigationArea = new HBox();

      {
        // gp.setGridLinesVisible(true);
        gp.setMinSize(1, 1);

        getChildren().add(gp);

        gp.at(3, 3).styleClass("navigation-area").add(navigationArea);

        // left overlay:
        gp.at(1, 1).spanning(1, 5).align(VPos.TOP).styleClass("overlay-panel").add(leftOverlayPanel);

        leftOverlayPanel.getChildren().addListener((Observable o) -> fadeIn(leftOverlayPanel));

        // right overlay:
        gp.at(5, 1).spanning(2, 5).align(VPos.TOP).styleClass("overlay-panel", "slide-in-right-panel").add(rightOverlayPanel);

        rightOverlayPanel.getChildren().addListener((Observable o) -> slideRight(rightOverlayPanel));

        setupArea(Area.CENTER_TOP, gp, (p, n) -> p.at(3, 1).add(n));
        setupArea(Area.NAVIGATION, navigationArea);
        setupArea(Area.NAME, gp, (p, n) -> p.at(3, 5).align(HPos.CENTER).align(VPos.TOP).add(n));
        setupArea(Area.DETAILS, gp, (p, n) -> p.at(5, 1).add(n));
        setupArea(Area.CENTER, gp, (p, n) -> p.at(3, 1).spanning(1, 5).align(VPos.BOTTOM).add(n));
        setupArea(Area.CONTEXT_PANEL, leftOverlayPanel);
        setupArea(Area.PREVIEW_PANEL, rightOverlayPanel);
        setupArea(Area.INFORMATION_PANEL, gp, (p, n) -> p.at(1, 6).spanning(5, 1).align(VPos.BOTTOM).align(HPos.LEFT).styleClass("information-panel").add(n));
      }

      private void slideRight(Pane pane) {
        int size = pane.getChildren().size();

        for(int i = size - 1; i >= 0; i--) {
          Node node = pane.getChildren().get(i);
          Transition transition = (Transition)node.getProperties().get("entity-layout.slide-right-transition");

          if(transition == null) {
            node.setVisible(false);
            node.setManaged(false);
//            node.setTranslateX(10000); // transition doesn't play immediately, so make sure the node isn't visible immediately by moving it far away

            transition = new SlideInTransition(pane, node, Duration.millis(2000), Duration.millis(500));
            transition.play();
          }

          if(i < size - 1 && transition instanceof SlideInTransition) {
            transition.stop();

            if(!node.isManaged()) {  // If panel not even visible yet, remove it immediately
              Platform.runLater(() -> pane.getChildren().remove(node));
            }
            else {
              transition = new SlideOutTransition(pane, node);
              transition.setOnFinished(e -> pane.getChildren().remove(node));
              transition.play();
            }
          }

          node.getProperties().put("entity-layout.slide-right-transition", transition);
        }
      }

      private void fadeIn(Pane pane) {
        int size = pane.getChildren().size();

        for(int i = size - 1; i >= 0; i--) {
          Node node = pane.getChildren().get(i);
          Timeline timeline = (Timeline)node.getProperties().get("entity-layout.fade-in-timeline");

          if(timeline == null) {
            timeline = new Timeline(
              new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 0)),
              new KeyFrame(Duration.seconds(0.01), new KeyValue(node.opacityProperty(), 0)),
              new KeyFrame(Duration.seconds(0.5), new KeyValue(node.opacityProperty(), 1.0, Interpolator.EASE_OUT))
            );

            timeline.play();
          }

          if(i < size - 1 && timeline.getKeyFrames().size() != 2) {
            timeline.stop();

            timeline = new Timeline(
              new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), node.getOpacity())),
              new KeyFrame(Duration.seconds(0.5), e -> {
                pane.getChildren().remove(node);
              }, new KeyValue(node.opacityProperty(), 0, Interpolator.EASE_IN))
            );

            timeline.play();
          }

          node.getProperties().put("entity-layout.fade-in-timeline", timeline);
        }
      }
    };

    areaPane.setMinSize(1, 1);
    areaPane.getStylesheets().add(LessLoader.compile(getClass().getResource("styles.less")).toExternalForm());

    configurePanes(areaPane, presentation);

    return areaPane;
  }

  private void setupStatusBar(AreaPane2<Area> areaPane, GridViewPresentation<T> presentation, ObservableList<MediaItem<T>> filteredItems, ObservableList<MediaItem<T>> originalItems) {
    StringBinding binding = Bindings.createStringBinding(() -> String.format(filteredItems.size() == originalItems.size() ? RESOURCES.getText("status-message.unfiltered") : RESOURCES.getText("status-message.filtered"), filteredItems.size(), originalItems.size()), filteredItems, originalItems);
    GridPane gridPane = new GridPane();
    VBox vbox = Containers.vbox("status-bar", Labels.create("total", binding), gridPane);

    vbox.getStylesheets().add(LessLoader.compile(getClass().getResource("status-bar.less")).toExternalForm());

    areaPane.add(Area.INFORMATION_PANEL, vbox);

    gridPane.at(0, 0).add(Containers.hbox("header-with-shortcut", Labels.create(RESOURCES.getText("header.order"), "header"), Labels.create("remote-shortcut, red")));
    gridPane.at(0, 1).add(Labels.create("status-bar-element", Binds.monadic(presentation.sortOrder).map(so -> RESOURCES.getText("sort-order", so.resourceKey)).orElse("Unknown")));
    gridPane.at(1, 0).add(Containers.hbox("header-with-shortcut", Labels.create(RESOURCES.getText("header.filter"), "header"), Labels.create("remote-shortcut, green")));
    gridPane.at(1, 1).add(Labels.create("status-bar-element", Binds.monadic(presentation.filter).map(f -> RESOURCES.getText("filter", f.resourceKey)).orElse("Unknown")));

    if(presentation.availableStateFilters.size() > 1) {
      gridPane.at(2, 0).add(Containers.hbox("header-with-shortcut", Labels.create(RESOURCES.getText("header.stateFilter"), "header"), Labels.create("remote-shortcut, yellow")));
      gridPane.at(2, 1).add(Labels.create("status-bar-element", Binds.monadic(presentation.stateFilter).map(sf -> RESOURCES.getText("stateFilter", sf.name().toLowerCase())).orElse("Unknown")));
    }
    if(presentation.availableGroups.size() > 1) {
      gridPane.at(3, 0).add(Containers.hbox("header-with-shortcut", Labels.create(RESOURCES.getText("header.group"), "header"), Labels.create("remote-shortcut, blue")));
      gridPane.at(3, 1).add(Labels.create("status-bar-element", Binds.monadic(presentation.group).map(g -> RESOURCES.getText("group", g.resourceKey)).orElse("Unknown")));
    }
  }

  protected abstract void onItemSelected(ItemSelectedEvent<MediaItem<T>> event, P presentation);
  protected abstract void configureCellFactory(MediaGridViewCellFactory<T> cellFactory);

  protected Node createContextPanel(@SuppressWarnings("unused") P presentation) {
    return null;
  }
}
