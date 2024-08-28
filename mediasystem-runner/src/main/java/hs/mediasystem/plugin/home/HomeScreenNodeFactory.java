package hs.mediasystem.plugin.home;

import hs.mediasystem.plugin.home.HomePresentation.OptionsPresentation;
import hs.mediasystem.plugin.library.scene.base.BackgroundPane;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.dialog.Dialogs;
import hs.mediasystem.runner.dialog.Tasks;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.control.ActionListView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.GridPaneUtil;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.ui.carousel.CarouselListCell;
import hs.mediasystem.util.javafx.ui.carousel.CarouselSkin;
import hs.mediasystem.util.javafx.ui.carousel.LinearLayout;
import hs.mediasystem.util.javafx.ui.transition.TransitionPane;
import hs.mediasystem.util.javafx.ui.transition.domain.EffectList;
import hs.mediasystem.util.javafx.ui.transition.effects.Fade;
import hs.mediasystem.util.javafx.ui.transition.effects.Slide;
import hs.mediasystem.util.javafx.ui.transition.effects.Slide.Direction;
import hs.mediasystem.util.javafx.ui.transition.multi.Custom;

import java.util.List;
import java.util.function.Supplier;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HomeScreenNodeFactory implements NodeFactory<HomePresentation> {
  record RootItem<P extends OptionsPresentation>(
    String name,
    Class<P> presentationClass,
    Supplier<P> presentationSupplier
  ) {}

  private static final String STYLES_URL = LessLoader.compile(HomeScreenNodeFactory.class, "styles.less");

  @Inject private CollectionsPresentation.Factory collectionsPresentationFactory;
  @Inject private CollectionsNodeFactory collectionsNodeFactory;
  @Inject private NewItemsPresentation.Factory newItemsPresentationFactory;
  @Inject private NewItemsNodeFactory newItemsNodeFactory;
  @Inject private RecommendationsPresentation.Factory recommendationsPresentationFactory;
  @Inject private RecommendationsNodeFactory recommendationsNodeFactory;
  @Inject private GeneralOptionsNodeFactory generalOptionsNodeFactory;

  private List<RootItem<? extends OptionsPresentation>> rootItems;

  @PostConstruct
  private void postConstruct() {
    this.rootItems = List.of(
      new RootItem<>("Home", RecommendationsPresentation.class, recommendationsPresentationFactory::create),
      new RootItem<>("Collections", CollectionsPresentation.class, collectionsPresentationFactory::create),
      new RootItem<>("New", NewItemsPresentation.class, newItemsPresentationFactory::create),
      new RootItem<>("Options", GeneralOptionsPresentation.class, GeneralOptionsPresentation::new)
    );
  }

  @Override
  public Node create(HomePresentation presentation) {
    GridPane grid = GridPaneUtil.create(new double[] {10, 5, 5, 10, 10, 10, 10, 10, 10, 10, 10}, new double[] {10, 10, 10, 10, 10, 10, 10, 10, 10, 10});

    BackgroundPane bgPane = new BackgroundPane();
    StackPane backdropContainer = Containers.stack("backdrop-container");
    StackPane clip = Containers.stack("clip");

    bgPane.getStyleClass().add("background-image");
    backdropContainer.getChildren().addAll(bgPane, clip);
    bgPane.backdropProperty().bind(presentation.backdrop.when(Nodes.showing(bgPane)));

    TransitionPane optionContainer = new TransitionPane(new Custom(
      Duration.millis(500),
      new EffectList(Duration.millis(500), List.of(new Slide(Interpolator.EASE_BOTH, Direction.DOWN), new Fade())),
      new EffectList(Duration.millis(500), List.of(new Slide(Interpolator.EASE_BOTH, Direction.UP), new Fade()))
    ));

    optionContainer.getStyleClass().add("option-container");

    ListView<String> menuListView = createMenu();

    menuListView.setFocusTraversable(false);  // ensure the vertical list does not get focus by accident; keyboard input should arrive in the horizontal carousel, which uses left/right while grid parent handles up/down

    grid.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if(e.getCode().isNavigationKey()) {
        int index = menuListView.getSelectionModel().getSelectedIndex();
        int newIndex = index;

        if(KeyCode.UP == e.getCode() && index > 0) {
          newIndex--;
        }
        else if(KeyCode.DOWN == e.getCode() && index < menuListView.getItems().size() - 1) {
          newIndex++;
        }

        if(index != newIndex) {
          int finalNewIndex = newIndex;

          Dialogs.showProgressDialog(e, Tasks.of(() -> rootItems.get(finalNewIndex).presentationSupplier.get()))
            .ifPresent(presentation.optionsPresentation::set);

          e.consume();
        }
      }
    });

    presentation.optionsPresentation.subscribe(op -> {
      ActionListView<?> listView = switch(op) {
        case RecommendationsPresentation p -> recommendationsNodeFactory.create(p);
        case CollectionsPresentation p -> collectionsNodeFactory.create(p);
        case NewItemsPresentation p -> newItemsNodeFactory.create(p);
        case GeneralOptionsPresentation p -> generalOptionsNodeFactory.create();
      };

      VBox.setVgrow(listView, Priority.ALWAYS);

      int menuIndex = indexOfPresentation(op);
      boolean invert = menuListView.getItems().indexOf(menuListView.getSelectionModel().getSelectedItem()) > menuIndex;

      optionContainer.add(invert, Containers.vbox("menu-view", listView));

      menuListView.getSelectionModel().select(menuIndex);
    });

    Label menuBackgroundLabel = Labels.create("menu-background", ">");
    StackPane.setAlignment(menuBackgroundLabel, Pos.CENTER_LEFT);

    grid.at(1, 0).spanning(10, 9).add(backdropContainer);
    grid.at(0, 5).spanning(11, 5).add(optionContainer);
    grid.at(0, 1).spanning(2, 5).add(Containers.stack("main-menu-container", menuBackgroundLabel, menuListView));

    grid.getStylesheets().add(STYLES_URL);

    return grid;
  }

  private int indexOfPresentation(OptionsPresentation presentation) {
    for(int i = 0; i < rootItems.size(); i++) {
      if(rootItems.get(i).presentationClass.equals(presentation.getClass())) {
        return i;
      }
    }

    throw new IndexOutOfBoundsException();
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

    ListView<String> listView = new ListView<>(FXCollections.observableArrayList(rootItems.stream().map(RootItem::name).toList()));
    CarouselSkin<String> skin = new CarouselSkin<>(listView);

    listView.setCellFactory(new MenuCellFactory());
    listView.setSkin(skin);

    skin.layoutProperty().set(linearLayout);

    return listView;
  }
}

