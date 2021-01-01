package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.work.Collection;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.plugin.cell.AnnotatedImageCellFactory;
import hs.mediasystem.plugin.home.OptionsNodeFactory.Option;
import hs.mediasystem.plugin.library.scene.base.BackgroundPane;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.ui.api.CollectionClient;
import hs.mediasystem.ui.api.domain.Recommendation;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.ResourceImageHandle;
import hs.mediasystem.util.Tuple;
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

import java.util.List;
import java.util.Objects;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
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
  @Inject private CollectionClient collectionClient;
  @Inject private NewItemsPresentation.Factory newItemsPresentationFactory;
  @Inject private NewItemsNodeFactory newItemsNodeFactory;
  @Inject private RecommendationsPresentation.Factory recommendationsPresentationFactory;
  @Inject private RecommendationsNodeFactory recommendationsNodeFactory;
  @Inject private OptionsNodeFactory optionsNodeFactory;

  @Override
  public Node create(HomePresentation presentation) {
    GridPane grid = GridPaneUtil.create(new double[] {10, 5, 5, 10, 10, 10, 10, 10, 10, 10, 10}, new double[] {10, 10, 10, 10, 10, 10, 10, 10, 10, 10});

    BackgroundPane bgPane = new BackgroundPane();
    StackPane backdropContainer = Containers.stack("backdrop-container");
    StackPane clip = Containers.stack("clip");

    bgPane.getStyleClass().add("background-image");
    backdropContainer.getChildren().addAll(bgPane, clip);

    TransitionPane optionContainer = new TransitionPane(new Custom(
      Duration.millis(500),
      new EffectList(Duration.millis(500), List.of(new Slide(Interpolator.EASE_BOTH, Direction.DOWN), new Fade())),
      new EffectList(Duration.millis(500), List.of(new Slide(Interpolator.EASE_BOTH, Direction.UP), new Fade()))
    ));

    optionContainer.getStyleClass().add("option-container");

    ListView<String> menuListView = createMenu();

    grid.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
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
      else if(current.equals("Options")) {
        listView = createOptionsView(bgPane.backdropProperty());
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
      .conditionOnShowing(menuListView)
      .subscribe(t -> {
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

    grid.at(1, 0).spanning(10, 9).add(backdropContainer);
    grid.at(0, 5).spanning(11, 5).add(optionContainer);
    grid.at(0, 1).spanning(2, 5).add(Containers.stack("main-menu-container", menuBackgroundLabel, menuListView));

    grid.getStylesheets().add(LESS_LOADER.compile("styles.less"));

    return grid;
  }

  private ActionListView<Recommendation> createWatchRecommendationView(ObjectProperty<ImageHandle> backdrop) {
    RecommendationsPresentation presentation = recommendationsPresentationFactory.create();
    ActionListView<Recommendation> mediaGridView = recommendationsNodeFactory.create(presentation);

    backdrop.bind(Val.wrap(presentation.selectedItem)
      .map(r -> r.getWork().getParent().filter(p -> p.getType().isSerie()).flatMap(Parent::getBackdrop).or(() -> r.getWork().getDetails().getBackdrop()).orElse(null))
      .map(imageHandleFactory::fromURI)
    );

    return mediaGridView;
  }

  private ActionListView<Collection> createCollectionView(ObjectProperty<ImageHandle> backdrop) {
    ActionListView<Collection> mediaGridView = new HorizontalCarousel<>(
      collectionClient.findCollections(),
      e -> PresentationLoader.navigate(e, () -> collectionPresentationProvider.createPresentation(e.getItem().getDefinition().getType(), e.getItem().getDefinition().getTag())),
      new AnnotatedImageCellFactory<>(this::fillCollectionModel)
    );

    backdrop.bind(Val.wrap(mediaGridView.getSelectionModel().selectedItemProperty())
      .map(c -> c.getBackdrop().orElse(null))
      .map(imageHandleFactory::fromURI)
    );

    return mediaGridView;
  }

  private ActionListView<NewItemsPresentation.Item> createNewView(ObjectProperty<ImageHandle> backdrop) {
    NewItemsPresentation presentation = newItemsPresentationFactory.create();
    ActionListView<NewItemsPresentation.Item> mediaGridView = newItemsNodeFactory.create(presentation);

    backdrop.bind(Val.wrap(presentation.selectedItem)
      .map(item -> item.recommendation.getWork().getParent().filter(p -> p.getType().isSerie()).flatMap(Parent::getBackdrop).or(() -> item.recommendation.getWork().getDetails().getBackdrop()).orElse(null))
      .map(imageHandleFactory::fromURI)
    );

    return mediaGridView;
  }

  private ActionListView<Option> createOptionsView(ObjectProperty<ImageHandle> backdrop) {
    ActionListView<Option> mediaGridView = optionsNodeFactory.create();

    backdrop.unbind();
    backdrop.set(new ResourceImageHandle(HomeScreenNodeFactory.class, "options-backdrop.jpg"));

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
          super.updateItem(item, empty);

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

    ListView<String> listView = new ListView<>(FXCollections.observableArrayList("Home", "Collections", "New", "Options"));
    CarouselSkin<String> skin = new CarouselSkin<>(listView);

    listView.setCellFactory(new MenuCellFactory());
    listView.setSkin(skin);

    skin.layoutProperty().set(linearLayout);

    return listView;
  }

  private void fillCollectionModel(Collection collection, AnnotatedImageCellFactory.Model model) {
    model.parentTitle.set(null);
    model.title.set(collection.getTitle());
    model.subtitle.set(null);
    model.sequence.set(null);
    model.imageHandle.set(collection.getCover().map(imageHandleFactory::fromURI).orElse(null));
    model.watchedFraction.set(-1);
    model.age.set(null);
  }
}

