package hs.mediasystem.plugin.library.scene.grid.common;

import hs.mediasystem.plugin.cell.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.base.ContextLayout;
import hs.mediasystem.plugin.library.scene.grid.common.GridViewPresentationFactory.GridViewPresentation;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.Theme;
import hs.mediasystem.runner.grouping.WorksGroup;
import hs.mediasystem.runner.presentation.Presentations;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.grid.MediaGridView;
import hs.mediasystem.ui.api.ConsumedStateChanged;
import hs.mediasystem.util.javafx.base.ItemSelectedEvent;
import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.control.AreaPane2;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.GridPaneUtil;
import hs.mediasystem.util.javafx.ui.gridlistviewskin.GridListViewSkin.GroupDisplayMode;
import hs.mediasystem.util.javafx.ui.transition.StandardTransitions;
import hs.mediasystem.util.javafx.ui.transition.TransitionPane;
import hs.mediasystem.util.javafx.ui.transition.domain.EffectList;
import hs.mediasystem.util.javafx.ui.transition.effects.Fade;
import hs.mediasystem.util.javafx.ui.transition.effects.Slide;
import hs.mediasystem.util.javafx.ui.transition.effects.Slide.Direction;
import hs.mediasystem.util.javafx.ui.transition.multi.Custom;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import javax.inject.Inject;

import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.value.Val;

public abstract class AbstractSetup<T, U, P extends GridViewPresentation<T, U>> implements NodeFactory<P> {
  protected static final String SYSTEM_PREFIX = "MediaSystem:Library:Presentation:";

  private static final String STYLES_URL = LessLoader.compile(AbstractSetup.class, "styles.less");

  @Inject private ContextLayout contextLayout;
  @Inject private Function<ObservableValue<?>, WorkCellPresentation> workCellPresentationFactory;
  @Inject private ViewStatusBarFactory viewStatusBarFactory;
  @Inject private Theme theme;

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

  private MediaGridView<U> createMediaGridView(P presentation) {
    MediaGridView<U> listView = new MediaGridView<>();

    listView.setOrientation(Orientation.VERTICAL);
    listView.pageByGroup.set(true);
    listView.groupDisplayMode.set(GroupDisplayMode.FOCUSED);

    listView.getStyleClass().add("glass-pane");
    listView.onItemSelected.set(e -> {
      if(!(e.getItem() instanceof WorksGroup wg) || wg.getChildren().isEmpty()) {
        @SuppressWarnings("unchecked")
        ItemSelectedEvent<T> itemSelectedEvent = (ItemSelectedEvent<T>)e;

        onItemSelected(itemSelectedEvent, presentation);
      }
      else {
        presentation.contextItem.unbind();
        presentation.contextItem.setValue(e.getItem());
      }
    });

    listView.addEventHandler(ConsumedStateChanged.ANY, e -> new Thread(() -> Platform.runLater(presentation.createUpdateTask())).start());

    Presentations.associate(listView, workCellPresentationFactory.apply(presentation.selectedItem));

    MediaGridViewCellFactory<U> cellFactory = new MediaGridViewCellFactory<>(AbstractSetup.this::fillModel);

    cellFactory.setMaxRatio(0.9);
    cellFactory.setContentBias(Orientation.VERTICAL);

    listView.setCellFactory(cellFactory);
    listView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> {
      if(current != null) {
        presentation.selectItem(current);
      }
    });

    EventStreams.valuesOf(Nodes.showing(listView))
      .map(visible -> visible ? presentation.items : FXCollections.<U>emptyObservableList())
      .feedTo(listView.itemsProperty());

    EventStreams.valuesOf(listView.itemsProperty())
      .conditionOnShowing(listView)
      .observe(new Consumer<ObservableList<U>>() {
        private Subscription subscription;

        @Override
        public void accept(ObservableList<U> list) {
          if(subscription != null) {
            subscription.unsubscribe();
          }

          subscription = EventStreams.valuesOf(presentation.selectedItem)
            .withDefaultEvent(presentation.selectedItem.getValue())
            .repeatOn(EventStreams.changesOf(list))
            .conditionOnShowing(listView)
            .observe(item -> updateSelectedItem(listView, presentation, item));
        }
      });

    return listView;
  }

  private void configurePanes(AreaPane2<Area> areaPane, TransitionPane contextPanel, TransitionPane previewPanel, P presentation) {
    MediaGridView<U> listView = createMediaGridView(presentation);

    // Clear preview panel immediately:
    Val.wrap(listView.getSelectionModel().selectedItemProperty()).values()
      .observe(current -> previewPanel.clear());

    // Create it if selection was stable for a time:
    Val.wrap(listView.getSelectionModel().selectedItemProperty()).values()
      .successionEnds(java.time.Duration.ofMillis(500))
      .observe(current -> {
        if(current != null) {
          Node context = createPreviewPanel(current);

          if(context != null) {
            previewPanel.add(context);
          }
        }
      });

    areaPane.add(Area.CENTER, listView);

    EventStreams.invalidationsOf(presentation.contextItem)
      .withDefaultEvent(null)
      .conditionOnShowing(listView)
      .observe(ci -> {
        Node contextItem = createContextPanel(presentation);

        if(contextItem != null) {
          contextPanel.add(contextItem);
        }
        else {
          contextPanel.clear();
        }
      });

    EventStreams.invalidationsOf(presentation.groups)
      .withDefaultEvent(null)
      .conditionOnShowing(listView)
      .map(x -> presentation.groups)
      .map(list -> list.isEmpty() ? null : list)
      .feedTo(listView.groups);

    listView.requestFocus();

    areaPane.add(Area.INFORMATION_PANEL, viewStatusBarFactory.create(presentation));
  }

  private void updateSelectedItem(MediaGridView<U> listView, P presentation, U selectedItem) {
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
    TransitionPane rightOverlayPanel = new TransitionPane(new Custom(
      Duration.millis(2000),
      new EffectList(Duration.millis(500), List.of(new Slide(Interpolator.EASE_OUT, Direction.RIGHT), new Fade())),
      new EffectList(Duration.millis(500), List.of(new Slide(Interpolator.EASE_IN, Direction.RIGHT), new Fade()))
    ));

    TransitionPane leftOverlayPanel = new TransitionPane(StandardTransitions.fade());

    AreaPane2<Area> areaPane = new AreaPane2<>() {
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
    areaPane.getStylesheets().add(STYLES_URL);

    configurePanes(areaPane, leftOverlayPanel, rightOverlayPanel, presentation);

    return areaPane;
  }

  private void onItemSelected(ItemSelectedEvent<T> event, P presentation) {
    theme.targetFor(presentation).ifPresent(target -> target.go(event));
  }

  protected abstract Node createPreviewPanel(U item);

  protected abstract void fillModel(U item, MediaGridViewCellFactory.Model model);

  protected Node createContextPanel(P presentation) {
    Object item = presentation.contextItem.getValue();

    return item == null ? null : contextLayout.createGeneric(item);
  }
}