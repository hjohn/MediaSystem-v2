package hs.mediasystem.plugin.home;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.base.BackgroundPane;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.plugin.library.scene.view.PresentationLoader;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.db.CollectionService;
import hs.mediasystem.runner.db.Recommendation;
import hs.mediasystem.runner.db.RecommendationService;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.control.ActionListView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.GridPaneUtil;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.carousel.CarouselListCell;
import hs.mediasystem.util.javafx.control.carousel.CarouselSkin;
import hs.mediasystem.util.javafx.control.carousel.LinearLayout;
import hs.mediasystem.util.javafx.control.transitionpane.TransitionPane;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Lighting;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Val;

@Singleton
public class HomeScreenNodeFactory implements NodeFactory<HomePresentation> {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private MediaItem.Factory mediaItemFactory;
  @Inject private CollectionPresentationProvider collectionPresentationProvider;
  @Inject private ProductionPresentation.Factory productionPresentationFactory;
  @Inject private MenuOptionCellFactory menuOptionCellFactory;
  @Inject private RecommendationService recommendationService;
  @Inject private CollectionService collectionService;

  @Override
  public Node create(HomePresentation presentation) {
    GridPane grid = GridPaneUtil.create(new double[] {10, 5, 5, 10, 10, 10, 10, 10, 10, 10, 10}, new double[] {10, 10, 10, 10, 10, 10, 10, 10, 10, 10});

    BackgroundPane bgPane = new BackgroundPane();
    StackPane backdropContainer = Containers.stack("backdrop-container");
    StackPane clip = Containers.stack("clip");

    bgPane.getStyleClass().add("background-image");
    backdropContainer.getChildren().addAll(bgPane, clip);

    grid.at(1, 0).spanning(10, 9).add(backdropContainer);

    TransitionPane optionContainer = new TransitionPane(new TransitionPane.FadeIn(), null);

    optionContainer.getStyleClass().add("option-container");

    grid.at(0, 5).spanning(11, 5).add(optionContainer);

    ListView<String> menuListView = createMenu();

    optionContainer.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
      if(e.getCode().isNavigationKey()) {
        int index = menuListView.getSelectionModel().getSelectedIndex();

        if(KeyCode.UP == e.getCode() && index > 0) {
          menuListView.getSelectionModel().select(index - 1);
          e.consume();
        }
        else if(KeyCode.DOWN == e.getCode() && index < menuListView.getItems().size() - 1) {
          menuListView.getSelectionModel().select(index + 1);
          e.consume();
        }
      }
    });

    menuListView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> {
      ActionListView<MenuOption> listView;

      if(current.equals("Home")) {
        listView = createWatchRecommendationView();
      }
      else if(current.equals("Collections")) {
        listView = createCollectionView();
      }
      else if(current.equals("New")) {
        listView = createNewView();
      }
      else {
        listView = null;
      }

      if(listView != null) {
        VBox.setVgrow(listView, Priority.ALWAYS);

        listView.onItemSelected.set(e -> PresentationLoader.navigate(e, listView.getSelectionModel().getSelectedItem().getPresentationSupplier()));

        optionContainer.getChildren().add(Containers.vbox("menu-view", listView));

        bgPane.backdropProperty().bind(Val.wrap(listView.getSelectionModel().selectedItemProperty())
          .map(o -> o.getBackdrop().orElse(null))
          .map(imageHandleFactory::fromURI)
        );
      }
    });

    menuListView.getSelectionModel().select(0);

    Label menuBackgroundLabel = Labels.create(">", "menu-background");
    StackPane.setAlignment(menuBackgroundLabel, Pos.CENTER_LEFT);

    grid.at(0, 0).spanning(2, 8).add(Containers.stack("main-menu-container", menuBackgroundLabel, menuListView));

    StackPane root = Containers.stack("media-look", grid);

    root.getStylesheets().add(LessLoader.compile(getClass().getResource("styles.less")).toExternalForm());

    return root;
  }

  private Supplier<ProductionPresentation> createProductionPresentationSupplier(Recommendation recommendation) {
    MediaDescriptor mediaDescriptor = recommendation.getParent().orElse(recommendation.getMediaDescriptor());

    @SuppressWarnings("unchecked")
    MediaItem<? extends Production> mediaItem = (MediaItem<? extends Production>)(MediaItem<?>)mediaItemFactory.create(mediaDescriptor, null);

    return () -> productionPresentationFactory.create(mediaItem);
  }

  private ActionListView<MenuOption> createWatchRecommendationView() {
    ActionListView<MenuOption> mediaGridView = createCarousel(
      recommendationService.findRecommendations(100).stream().map(r -> new RecommendationMenuOptionAdapter(r, createProductionPresentationSupplier(r))).collect(Collectors.toList()),
      menuOptionCellFactory
    );

    return mediaGridView;
  }

  private ActionListView<MenuOption> createCollectionView() {
    ActionListView<MenuOption> mediaGridView = createCarousel(
      collectionService.findCollections().stream()
        .map(c -> new CollectionMenuOptionAdapter(c, () -> collectionPresentationProvider.createPresentation(c.getDefinition().getType(), c.getDefinition().getTag())))
        .collect(Collectors.toList()),
      menuOptionCellFactory
    );

    return mediaGridView;
  }

  private ActionListView<MenuOption> createNewView() {
    ActionListView<MenuOption> mediaGridView = createCarousel(
      recommendationService.findNew().stream().map(r -> new RecommendationMenuOptionAdapter(r, createProductionPresentationSupplier(r))).collect(Collectors.toList()),
      menuOptionCellFactory
    );

    return mediaGridView;
  }

  private final class MenuCellFactory implements Callback<ListView<String>, ListCell<String>> {
    @Override
    public CarouselListCell<String> call(ListView<String> view) {
      return new CarouselListCell<>() {
        private Timeline activeTimeline;

        {
          Glow glow = new Glow(0.0);
          Lighting lighting = new Lighting();
          ColorAdjust colorAdjust = new ColorAdjust(0, 0.3, 0, 0);

          lighting.setDiffuseConstant(0.6);
          lighting.setSurfaceScale(1.5);

          glow.setInput(lighting);
          lighting.setContentInput(colorAdjust);

          additionalEffectProperty().set(glow);
          focusedProperty().addListener((obs, old, current) -> {
            if(activeTimeline != null) {
              activeTimeline.stop();
            }

            if(current) {
              activeTimeline = new Timeline(
                new KeyFrame(
                  Duration.seconds(0.5),
                  new KeyValue(zoomProperty(), 1.5),
                  new KeyValue(glow.levelProperty(), 0.7),
                  new KeyValue(lighting.diffuseConstantProperty(), 1.1),
                  new KeyValue(lighting.surfaceScaleProperty(), 2.25)
                )
              );
            }
            else {
              activeTimeline = new Timeline(
                new KeyFrame(
                  Duration.seconds(0.5),
                  new KeyValue(zoomProperty(), 1.0),
                  new KeyValue(glow.levelProperty(), 0.0),
                  new KeyValue(lighting.diffuseConstantProperty(), 0.6),
                  new KeyValue(lighting.surfaceScaleProperty(), 1.5)
                )
              );
            }

            activeTimeline.play();
          });
        }

        protected void updateItem(String item, boolean empty) {
          if(!empty) {
            this.setText(item);
          }
        }
      };
    }
  }

  private ListView<String> createMenu() {
    LinearLayout linearLayout = new LinearLayout();

    linearLayout.reflectionEnabledProperty().set(false);
    linearLayout.cellAlignmentProperty().set(0.5);

    ListView<String> listView = new ListView<>(FXCollections.observableArrayList("Home", "Collections", "New"));
    CarouselSkin<String> skin = new CarouselSkin<>(listView);

    listView.setCellFactory(new MenuCellFactory());
    listView.setSkin(skin);

    skin.layoutProperty().set(linearLayout);

    return listView;
  }


  private static <T> ActionListView<T> createCarousel(List<T> items, Callback<ListView<T>, ListCell<T>> cellFactory) {
    ActionListView<T> listView = new ActionListView<>();

    listView.setCellFactory(cellFactory);
    listView.setItems(FXCollections.observableList(items));
    listView.getSelectionModel().select(0);
    listView.getFocusModel().focus(0);
    listView.setOrientation(Orientation.HORIZONTAL);

    CarouselSkin<T> skin = new CarouselSkin<>(listView);
//    RayLayout layout = new RayLayout() {
//      @Override
//      protected void customizeCell(RayCellIterator iterator) {
//        Point3D[] points = iterator.currentPoints();
//        Point3D center = new Point3D((points[0].getX() + points[1].getX()) * 0.5, 0, (points[0].getZ() + points[1].getZ()) * 0.5);
//
//        for(int i = 0; i < points.length; i++) {
//          points[i] = rotateY(points[i], center, Math.PI * 0.5);
//        }
//      }
//    };

    LinearLayout layout = new LinearLayout();

    listView.setSkin(skin);
    skin.setLayout(layout);
    skin.verticalAlignmentProperty().set(0.6);
    layout.centerPositionProperty().set(0.2);
    layout.maxCellWidthProperty().set(350);
    layout.maxCellHeightProperty().set(300);
    layout.densityProperty().set(0.0026);
    layout.reflectionEnabledProperty().set(true);
    layout.cellAlignmentProperty().set(1.0);
    layout.clipReflectionsProperty().set(true);
    layout.viewAlignmentProperty().set(0.9);

//    // Values for RayLayout
//    layout.densityProperty().set(0.0084);
//    layout.maxCellWidthProperty().set(350);
//    layout.maxCellHeightProperty().set(300);
//    layout.radiusRatioProperty().set(1.5);
//    layout.viewDistanceRatioProperty().set(1.6);
//    layout.centerPositionProperty().set(0.2);
//    layout.viewAlignmentProperty().set(0.8);
//    layout.carouselViewFractionProperty().set(0.35);
//    layout.cellAlignmentProperty().set(1.0);

//    // Show Stage with CarouselSkin controls
//    Scene scene = new Scene(new ControlPanel(layout));
//    Stage stage = new Stage(StageStyle.DECORATED);
//
//    stage.setScene(scene);
//    stage.show();

    return listView;
  }
}

