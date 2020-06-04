package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Collection;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.cell.AnnotatedImageCellFactory;
import hs.mediasystem.plugin.library.scene.base.BackgroundPane;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentation.State;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.ui.api.CollectionClient;
import hs.mediasystem.ui.api.RecommendationClient;
import hs.mediasystem.ui.api.domain.Recommendation;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.javafx.Nodes;
import hs.mediasystem.util.javafx.control.ActionListView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.GridPaneUtil;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.carousel.CarouselListCell;
import hs.mediasystem.util.javafx.control.carousel.CarouselSkin;
import hs.mediasystem.util.javafx.control.carousel.LinearLayout;
import hs.mediasystem.util.javafx.control.transition.EffectList;
import hs.mediasystem.util.javafx.control.transition.TransitionPane;
import hs.mediasystem.util.javafx.control.transition.effects.Fade;
import hs.mediasystem.util.javafx.control.transition.effects.Slide;
import hs.mediasystem.util.javafx.control.transition.effects.Slide.Direction;
import hs.mediasystem.util.javafx.control.transition.multi.Custom;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
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
import org.reactfx.value.Var;

@Singleton
public class HomeScreenNodeFactory implements NodeFactory<HomePresentation> {
  private static final LessLoader LESS_LOADER = new LessLoader(HomeScreenNodeFactory.class);

  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private CollectionPresentationProvider collectionPresentationProvider;
  @Inject private ProductionPresentation.Factory productionPresentationFactory;
  @Inject private RecommendationClient recommendationClient;
  @Inject private CollectionClient collectionClient;

  @Override
  public Node create(HomePresentation presentation) {
    GridPane grid = GridPaneUtil.create(new double[] {10, 5, 5, 10, 10, 10, 10, 10, 10, 10, 10}, new double[] {10, 10, 10, 10, 10, 10, 10, 10, 10, 10});

    BackgroundPane bgPane = new BackgroundPane();
    StackPane backdropContainer = Containers.stack("backdrop-container");
    StackPane clip = Containers.stack("clip");

    bgPane.getStyleClass().add("background-image");
    backdropContainer.getChildren().addAll(bgPane, clip);

    grid.at(1, 0).spanning(10, 9).add(backdropContainer);

    TransitionPane optionContainer = new TransitionPane(new Custom(
      Duration.millis(500),
      new EffectList(Duration.millis(500), List.of(new Slide(Interpolator.EASE_BOTH, Direction.DOWN), new Fade())),
      new EffectList(Duration.millis(500), List.of(new Slide(Interpolator.EASE_BOTH, Direction.UP), new Fade()))
    ));

    optionContainer.getStyleClass().add("option-container");

    grid.at(0, 5).spanning(11, 5).add(optionContainer);

    ListView<String> menuListView = createMenu();

    menuListView.setFocusTraversable(false);  // focus should never be here, horizontal carousel should always get it instead

    optionContainer.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
      if(e.getCode().isNavigationKey()) {
        int index = menuListView.getSelectionModel().getSelectedIndex();

        if(KeyCode.UP == e.getCode() && index > 0) {
          presentation.selectedItem.setValue(Tuple.of(menuListView.getItems().get(index - 1), 0));
          e.consume();
        }
        else if(KeyCode.DOWN == e.getCode() && index < menuListView.getItems().size() - 1) {
          presentation.selectedItem.setValue(Tuple.of(menuListView.getItems().get(index + 1), 0));
          e.consume();
        }
      }
    });

    Var<ActionListView<?>> activeListView = Var.newSimpleVar(null);

    menuListView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> {
      ActionListView<?> listView;

      boolean invert = menuListView.getItems().indexOf(old) > menuListView.getItems().indexOf(current);

      if(current.equals("Home")) {
        listView = createWatchRecommendationView(bgPane.backdropProperty());
      }
      else if(current.equals("Collections")) {
        listView = createCollectionView(bgPane.backdropProperty());
      }
      else if(current.equals("New")) {
        listView = createNewView(bgPane.backdropProperty());
      }
      else {
        listView = null;
      }

      if(listView != null) {
        VBox.setVgrow(listView, Priority.ALWAYS);

        optionContainer.add(invert, Containers.vbox("menu-view", listView));
      }

      activeListView.setValue(listView);
    });

    presentation.selectedItem.values()
      .repeatOn(Nodes.visible(menuListView).values())
      .conditionOnShowing(menuListView)
      .observe(t -> {
        menuListView.getSelectionModel().select(t.a);
        activeListView.filter(Objects::nonNull).ifPresent(lv -> lv.getSelectionModel().select(t.b));
      });

    activeListView.flatMap(lv -> lv.getSelectionModel().selectedIndexProperty()).values().observe(i -> {
      if(i != null) {
        presentation.selectedItem.setValue(Tuple.of(menuListView.getSelectionModel().getSelectedItem(), (Integer)i));
      }
    });

    Label menuBackgroundLabel = Labels.create("menu-background", ">");
    StackPane.setAlignment(menuBackgroundLabel, Pos.CENTER_LEFT);

    grid.at(0, 0).spanning(2, 8).add(Containers.stack("main-menu-container", menuBackgroundLabel, menuListView));

    StackPane root = Containers.stack("media-look", grid);

    root.getStylesheets().add(LESS_LOADER.compile("styles.less"));

    return root;
  }

  private Function<Recommendation, ProductionPresentation> createProductionPresentationFunction() {
    return r -> {
      boolean hasParent = r.getWork().getType().isComponent();
      WorkId id = hasParent ?
          r.getWork().getParent().map(Parent::getId).orElseThrow() :
          r.getWork().getId();

      return productionPresentationFactory.create(id, hasParent ? State.EPISODE : State.OVERVIEW, hasParent ? r.getWork().getId() : null);
    };
  }

  private ActionListView<Recommendation> createWatchRecommendationView(ObjectProperty<ImageHandle> backdrop) {
    ActionListView<Recommendation> mediaGridView = createCarousel(
      recommendationClient.findRecommendations(100),
      createProductionPresentationFunction(),
      new AnnotatedImageCellFactory<>(this::fillRecommendationModel)
    );

    backdrop.bind(Val.wrap(mediaGridView.getSelectionModel().selectedItemProperty())
      .map(r -> r.getWork().getParent().filter(p -> p.getType().isSerie()).flatMap(Parent::getBackdrop).or(() -> r.getWork().getDetails().getBackdrop()).orElse(null))
      .map(imageHandleFactory::fromURI)
    );

    return mediaGridView;
  }

  private ActionListView<Collection> createCollectionView(ObjectProperty<ImageHandle> backdrop) {
    ActionListView<Collection> mediaGridView = createCarousel(
      collectionClient.findCollections(),
      c -> collectionPresentationProvider.createPresentation(c.getDefinition().getType(), c.getDefinition().getTag()),
      new AnnotatedImageCellFactory<>(this::fillCollectionModel)
    );

    backdrop.bind(Val.wrap(mediaGridView.getSelectionModel().selectedItemProperty())
      .map(c -> c.getBackdrop().orElse(null))
      .map(imageHandleFactory::fromURI)
    );

    return mediaGridView;
  }

  private ActionListView<Recommendation> createNewView(ObjectProperty<ImageHandle> backdrop) {
    ActionListView<Recommendation> mediaGridView = createCarousel(
      recommendationClient.findNew(mediaType -> !mediaType.isComponent() && mediaType != MediaType.FOLDER && mediaType != MediaType.FILE),
      createProductionPresentationFunction(),
      new AnnotatedImageCellFactory<>(this::fillRecommendationModel)
    );

    backdrop.bind(Val.wrap(mediaGridView.getSelectionModel().selectedItemProperty())
      .map(r -> r.getWork().getParent().filter(p -> p.getType().isSerie()).flatMap(Parent::getBackdrop).or(() -> r.getWork().getDetails().getBackdrop()).orElse(null))
      .map(imageHandleFactory::fromURI)
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


  private static <T> ActionListView<T> createCarousel(List<T> items, Function<T, ? extends Presentation> presentationSupplier, Callback<ListView<T>, ListCell<T>> cellFactory) {
    ActionListView<T> listView = new ActionListView<>();

    listView.setCellFactory(cellFactory);
    listView.setItems(FXCollections.observableList(items));
    listView.getSelectionModel().select(0);
    listView.getFocusModel().focus(0);
    listView.setOrientation(Orientation.HORIZONTAL);
    listView.onItemSelected.set(e -> PresentationLoader.navigate(e, () -> presentationSupplier.apply(listView.getSelectionModel().getSelectedItem())));

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

  private void fillRecommendationModel(Recommendation recommendation, AnnotatedImageCellFactory.Model model) {
    boolean hasParent = recommendation.getWork().getType().isComponent();

    model.parentTitle.set(hasParent ? recommendation.getWork().getParent().map(Parent::getName).orElse(null) : null);
    model.title.set(recommendation.getWork().getDetails().getTitle());
    model.subtitle.set(!hasParent ? recommendation.getWork().getDetails().getReleaseDate().map(LocalDate::getYear).map(Object::toString).orElse(null) : null);
    model.sequence.set(recommendation.getWork().getDetails().getSequence()
      .map(seq -> seq.getSeasonNumber().map(s -> s + "x").orElse("") + seq.getNumber())
      .orElse(null)
    );
    model.imageHandle.set(recommendation.getWork().getDetails().getBackdrop().map(imageHandleFactory::fromURI).orElse(null));

    double fraction = recommendation.getLength().map(len -> recommendation.getPosition().toSeconds() / (double)len.toSeconds()).orElse(0.0);

    model.watchedFraction.set(recommendation.isWatched() ? 1.0 : fraction > 0 ? fraction : -1);
    model.age.set(Optional.of(recommendation.getLastTimeWatched())
      .map(i -> i.atZone(ZoneId.systemDefault()))
      .map(ZonedDateTime::toLocalDateTime)
      .map(SizeFormatter::formatTimeAgo)
      .orElse(null)
    );
  }

  private void fillCollectionModel(Collection collection, AnnotatedImageCellFactory.Model model) {
    model.parentTitle.set(null);
    model.title.set(collection.getTitle());
    model.subtitle.set(null);
    model.sequence.set(null);
    model.imageHandle.set(collection.getImage().map(imageHandleFactory::fromURI).orElse(null));
    model.watchedFraction.set(-1);
    model.age.set(null);
  }
}

