package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Release;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.plugin.library.scene.AspectCorrectLabel;
import hs.mediasystem.plugin.library.scene.MediaGridView;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.MediaItem.MediaStatus;
import hs.mediasystem.plugin.library.scene.MediaItemFormatter;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation.State;
import hs.mediasystem.plugin.library.scene.serie.SeasonBar.Entry;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewPresentation;
import hs.mediasystem.plugin.library.scene.view.PresentationLoader;
import hs.mediasystem.plugin.library.scene.view.ProductionCollectionPresentation;
import hs.mediasystem.plugin.library.scene.view.RecommendationsPresentation;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.TransitionPane;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.javafx.AsyncImageProperty3;
import hs.mediasystem.util.javafx.control.AutoVerticalScrollPane;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Buttons;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.StarRating;
import hs.mediasystem.util.javafx.control.gridlistviewskin.GridListViewSkin.GroupDisplayMode;
import hs.mediasystem.util.javafx.control.gridlistviewskin.Group;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.EventStreams;
import org.reactfx.util.Interpolator;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

@Singleton
public class ProductionOverviewNodeFactory implements NodeFactory<ProductionPresentation> {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private ProductionCollectionPresentation.Factory productionCollectionPresentationFactory;
  @Inject private RecommendationsPresentation.Factory recommendationsPresentationFactory;
  @Inject private CastAndCrewPresentation.Factory castAndCrewPresentationFactory;
  @Inject private ShowInfoEventHandler showInfoEventHandler;
  @Inject private CastPaneFactory castPaneFactory;

  @Override
  public Node create(ProductionPresentation presentation) {
    MainPanel mainPanel = new MainPanel(presentation);

    presentation.showInfo.conditionOnShowing(mainPanel).subscribe(e -> showInfoEventHandler.handle(e, presentation.state.get() == State.OVERVIEW ? presentation.rootItem : presentation.episodeItem.get()));

    return mainPanel;
  }

  private class MainPanel extends HBox {
    private final ProductionPresentation presentation;

    public MainPanel(ProductionPresentation presentation) {
      this.presentation = presentation;

      presentation.update();

      MediaItem<?> mediaItem = presentation.rootItem;
      AsyncImageProperty3 imageProperty = new AsyncImageProperty3(840, 840);

      imageProperty.imageHandleProperty().set(Optional.ofNullable(mediaItem.getProduction().getImage()).map(imageHandleFactory::fromURI).orElse(null));

      BiasedImageView poster = new BiasedImageView();

      poster.setOrientation(Orientation.VERTICAL);
      poster.imageProperty().bind(imageProperty);

      Label titleLabel = Labels.create("title", mediaItem.productionTitle, null);

      if(titleLabel.getText().length() > 40) {
        titleLabel.getStyleClass().add("smaller");
      }

      VBox leftTitleBox = Containers.vbox(
        titleLabel,
        Labels.create(createYear(mediaItem), "release-year"),
        Labels.create(mediaItem.getProduction().getGenres().stream().collect(Collectors.joining(" / ")), "genres")
      );

      TransitionPane dynamicBoxContainer = new TransitionPane(new TransitionPane.FadeIn(), createDynamicBox());

      HBox.setHgrow(leftTitleBox, Priority.ALWAYS);
      VBox.setVgrow(dynamicBoxContainer, Priority.ALWAYS);

      StackPane indicatorPane = createMediaStatusIndicatorPane(
        presentation.watchedPercentage.animate(Duration.ofSeconds(2), Interpolator.LINEAR_DOUBLE),
        presentation.missingFraction
      );

      VBox descriptionBox = new VBox(
        Containers.hbox(
          "title-panel",
          leftTitleBox,
          Containers.vbox(
            createStarRating(mediaItem.getProduction().getReception(), 20, 8),
            indicatorPane
          )
        ),
        dynamicBoxContainer
      );

      EventStreams.invalidationsOf(presentation.state)
        .conditionOnShowing(this)
        .subscribe(obs -> dynamicBoxContainer.add(createDynamicBox()));

      getChildren().addAll(poster, descriptionBox);

      HBox.setHgrow(descriptionBox, Priority.ALWAYS);

      getStyleClass().add("main-panel");
      getStylesheets().add(LessLoader.compile(getClass().getResource("styles.less")).toExternalForm());
    }

    private String createYear(MediaItem<?> mediaItem) {
      if(mediaItem.getData() instanceof Serie) {
        Serie serie = (Serie)mediaItem.getData();

        if(serie.getDate() == null) {
          return "";
        }

        String year = "" + serie.getDate().getYear();

        if(serie.getState() == Serie.State.ENDED && serie.getLastAirDate() != null && serie.getLastAirDate().getYear() != serie.getDate().getYear()) {
          year += " - " + serie.getLastAirDate().getYear();
        }
        else if(serie.getState() == Serie.State.CONTINUING) {
          year += " -";
        }

        return year;
      }

      return Optional.ofNullable(mediaItem.getProduction().getDate()).map(LocalDate::getYear).map(Object::toString).orElse("");
    }

    private VBox createDynamicBox() {
      VBox box = Containers.vbox("dynamic-panel");

      State state = presentation.state.get();
      MediaItem<?> productionItem = presentation.rootItem;

      switch(state) {
      case OVERVIEW:
        {
          BorderPane borderPane = new BorderPane();
          Production production = productionItem.getProduction();

          box.getChildren().add(borderPane);

          VBox leftBox = Containers.vbox();

          if(production instanceof Movie) {
            Movie movie = (Movie)production;

            if(movie.getTagLine() != null && !movie.getTagLine().isEmpty()) {
              leftBox.getChildren().add(Labels.create("“" + movie.getTagLine() + "”", "tag-line"));
            }
          }

          leftBox.getChildren().add(new AutoVerticalScrollPane(Labels.create(production.getDescription(), "description"), 12000, 40));

          Region castPane = castPaneFactory.create(production);

          leftBox.setPrefWidth(100);  // Limit pref width, so free space can be assigned according to Grow / Percentage settings
          castPane.setPrefWidth(100);  // Limit pref width, so free space can be assigned according to Grow / Percentage settings

          HBox.setHgrow(leftBox, Priority.ALWAYS);
          HBox.setHgrow(castPane, Priority.ALWAYS);

          HBox outer = Containers.hbox("overview-panel", leftBox, castPane);

          borderPane.setMinHeight(1);
          borderPane.setPrefHeight(1);
          borderPane.setCenter(outer);
          borderPane.setBottom(createButtons());
          borderPane.getStyleClass().add("overview-dynamic-panel");

          VBox.setVgrow(borderPane, Priority.ALWAYS);
        }
        break;
      case LIST:
        MediaGridView<MediaItem<Episode>> gridView = new MediaGridView<>();
        MediaGridViewCellFactory<Episode> cellFactory = new MediaGridViewCellFactory<>();

        gridView.visibleRows.set(1);
        gridView.visibleColumns.set(3);
        gridView.setOrientation(Orientation.HORIZONTAL);
        gridView.onItemSelected.set(e -> presentation.toEpisodeState());
        gridView.pageByGroup.set(true);
        gridView.showHeaders.set(false);
        gridView.scrollBarVisible.set(false);
        gridView.groupDisplayMode.set(GroupDisplayMode.FOCUSED);

        cellFactory.setTitleBindProvider(item -> item.productionTitle);
        cellFactory.setSideBarTopLeftBindProvider(item -> new SimpleStringProperty((item.getData().getSeasonNumber() == 0 ? "Special " : "Ep. ") + item.getData().getNumber()));
        cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
//        cellFactory.setSequenceNumberExtractor(item -> Optional.ofNullable(item.getData().getNumber()).map(i -> "" + i).orElse(null));
        cellFactory.setPlaceHolderAspectRatio(9.0 / 16.0);
        cellFactory.setMinRatio(4.0 / 3.0);
        cellFactory.setImageExtractor(item ->
          Optional.of(item).map(MediaItem::getRelease).map(Release::getImage)
            .or(() -> Optional.of(item).map(MediaItem::getStream).map(BasicStream::getId).map(StreamID::asInt).map(id -> new ImageURI("localdb://" + id + "/1")))
            .map(imageHandleFactory::fromURI)
            .orElse(null)
        );

        gridView.setCellFactory(cellFactory);
        gridView.setItems(FXCollections.observableList(presentation.episodeItems));

        VBox.setVgrow(gridView, Priority.ALWAYS);

        Set<Integer> knownSeasons = new HashSet<>();
        List<Group> groups = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        Map<Integer, Integer> seasonNumberToIndex = new HashMap<>();

        for(int i = 0; i < presentation.episodeItems.size(); i++) {
          MediaItem<Episode> episode = presentation.episodeItems.get(i);
          int seasonNumber = episode.getData().getSeasonNumber();

          if(!knownSeasons.contains(seasonNumber)) {
            groups.add(new Group(seasonNumber == 0 ? "Specials" : seasonNumber == -1 ? "Extras" : "Season " + seasonNumber, i));
            knownSeasons.add(seasonNumber);

            Entry entry;

            switch(seasonNumber) {
            case 0:
              seasonNumberToIndex.put(0, entries.size());
              entry = new Entry("Specials", null, false);
              break;
            case -1:
              seasonNumberToIndex.put(-1, entries.size());
              entry = new Entry("Extras", null, false);
              break;
            default:
              seasonNumberToIndex.put(seasonNumber, entries.size());
              entry = new Entry("" + seasonNumber, null, true);
            }

            Var<SeasonWatchState> var = presentation.getSeasonWatchStates().get(seasonNumber);

            entry.mediaStatus.bind(var.map(sws -> sws.totalEpisodes == sws.missingEpisodes ? MediaStatus.UNAVAILABLE : sws.totalEpisodes == sws.watchedEpisodes ? MediaStatus.WATCHED : MediaStatus.AVAILABLE));
            entries.add(entry);
          }
        }

        SeasonBar seasonsBar = new SeasonBar();

        seasonsBar.getStyleClass().add("season-bar");
        seasonsBar.setMinWidth(1);
        seasonsBar.setPrefWidth(1);
        seasonsBar.entries.setValue(entries);

        gridView.groups.set(groups);
        gridView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> seasonsBar.activeIndex.setValue(seasonNumberToIndex.get(current.getData().getSeasonNumber())));

        box.getChildren().addAll(seasonsBar, gridView);

        gridView.getSelectionModel().select(presentation.episodeItem.get());
        presentation.episodeItem.bind(gridView.getSelectionModel().selectedItemProperty());
        break;
      case EPISODE:
        buildEpisodeDynamicUI(box);
        break;
      }

      return box;
    }

    private void buildEpisodeDynamicUI(VBox box) {
      presentation.episodeItem.unbind();

      List<MediaItem<Episode>> episodes = presentation.episodeItems;
      TransitionPane transitionPane = new TransitionPane(new TransitionPane.Scroll(), buildEpisodeUI());
      BorderPane borderPane = new BorderPane();

      EventStreams.changesOf(presentation.episodeItem)
        .conditionOnShowing(this)
        .subscribe(c -> transitionPane.add(episodes.indexOf(c.getOldValue()) > episodes.indexOf(c.getNewValue()) ? 0 : -1, buildEpisodeUI()));

      borderPane.setCenter(transitionPane);
      borderPane.setBottom(createButtons());
      borderPane.getProperties().put("presentation2", new EpisodePresentation(presentation));
      borderPane.getStyleClass().add("episode-dynamic-panel");

      VBox.setVgrow(borderPane, Priority.ALWAYS);

      box.getChildren().add(borderPane);
    }

    private HBox buildEpisodeUI() {
      MediaItem<Episode> mediaItem = presentation.episodeItem.get();
      AsyncImageProperty3 imageProperty = new AsyncImageProperty3(840, 840);

      imageProperty.imageHandleProperty().set(Optional.ofNullable(mediaItem.getData().getImage()).map(imageHandleFactory::fromURI).orElse(null));

      Label label = new AspectCorrectLabel("?", 0.75, Orientation.VERTICAL, 1000, 1000);
      BiasedImageView poster = new BiasedImageView(label);

      poster.setOrientation(Orientation.VERTICAL);
      poster.imageProperty().bind(imageProperty);

      Val<Double> percentage = Val.combine(mediaItem.watched, mediaItem.missing, presentation.resume.resumePosition.orElseConst(0), presentation.totalDuration.orElseConst(1), (w, m, rp, td) -> {
        return w ? 1.0 :
               m ? -0.01 :
                   rp / (double)td;
      });

      StackPane indicatorPane = createMediaStatusIndicatorPane(percentage.animate(Duration.ofSeconds(2), Interpolator.EASE_BOTH_DOUBLE), Val.constant(0.0));

      poster.getOverlayRegion().getChildren().add(indicatorPane);

      String formattedDate = MediaItemFormatter.formattedLocalDate(mediaItem.getRelease().getDate());
      String subtitle = createSeasonEpisodeText(mediaItem) + (formattedDate == null ? "" : " • " + formattedDate);

      Label titleLabel = Labels.create("title", presentation.episodeItem.get().productionTitle);

      titleLabel.setMinHeight(Region.USE_PREF_SIZE);  // With reflowed labels, sometimes not enough vertical space is assigned and the reflow fails to use the next line and adds an ellipsis instead...

      VBox titleBox = Containers.vbox(
        titleLabel,
        Labels.create(subtitle, "subtitle")
      );

      HBox.setHgrow(titleBox, Priority.ALWAYS);

      VBox vbox = Containers.vbox(
        Containers.hbox(
          titleBox,
          createStarRating(mediaItem.getData().getReception(), 10, 4)
        ),
        new AutoVerticalScrollPane(Labels.create(mediaItem.getData().getDescription(), "description"), 12000, 40)
      );

      HBox hbox = Containers.hbox("episode-panel", vbox, poster);

      /*
       * The following four tweaks to the layout are needed to have the description box and poster play nice.
       *
       * The poster should take up as much space as possible, aspect ratio allowing, without "pushing" its parent
       * bigger (as that would resize the root of this control even).  The outer container that is returned therefore
       * gets its min and pref sizes forced to a lower arbirtrary values; min size to prevent the entire root to
       * become bigger; preferred size to prevent it stealing space from parent containers (like the primary left poster).
       *
       * Secondly, the description box gets its preferred width lowered, as Labels with long texts use a preferred
       * width that would fit their entire text instead.  Containers with such a Label are not smart enough to use
       * Label's content bias to find a more reasonable configuration by themselves.
       *
       * Finally, the description box is the only box allowed to take up left-over space.  Everything else will
       * get the exact space required, and no more.
       */

      HBox.setHgrow(vbox, Priority.ALWAYS);  // Description box can grow, poster however should just try and attain its preferred size (which can be huge).
      vbox.setPrefWidth(100);  // Limit pref width on the description box, so other content can be resized to their preferred sizes.
      hbox.setMinSize(100, 100);
      hbox.setPrefSize(100, 100);

      return hbox;
    }

    private StackPane createMediaStatusIndicatorPane(Val<Double> percentage, Val<Double> missingFraction) {
      MediaStatusIndicatorPane indicatorPane = new MediaStatusIndicatorPane();

      indicatorPane.setAlignment(Pos.BOTTOM_RIGHT);
      indicatorPane.getStyleClass().add("indicator-pane");
      indicatorPane.value.bind(percentage.conditionOnShowing(this));
      indicatorPane.missingFraction.bind(missingFraction.conditionOnShowing(this));

      return indicatorPane;
    }

    private void updateButtons(HBox hbox) {
      if(presentation.state.get() != State.LIST) {
        hbox.getChildren().clear();

        switch(presentation.buttonState.get()) {
        case MAIN:
          hbox.getChildren().addAll(
            presentation.rootItem.getData() instanceof Serie && presentation.state.get() == State.OVERVIEW ?
              Buttons.create("Episodes", e -> presentation.toListState()) :
                presentation.resume.enabledProperty().getValue() ?
                  Buttons.create("Play", e -> presentation.toPlayResumeButtonState()) :
                  Buttons.create(presentation.play),
            presentation.state.get() == State.OVERVIEW ?  // Only show Related for Movie and Serie, for Episode only Cast&Crew is available
              Buttons.create("Related", e -> presentation.toRelatedButtonState()) :
              Buttons.create("Cast & Crew", e -> navigateToCastAndCrew(e, presentation.episodeItem.get())),
            Buttons.create(presentation.playTrailer)
          );
          break;
        case PLAY_RESUME:
          hbox.getChildren().addAll(
            create("Resume", "From " + SizeFormatter.SECONDS_AS_POSITION.format(presentation.resume.resumePosition.getValue().longValue()), e -> presentation.resume.trigger(e)),
            create("Play", "From start", e -> presentation.play.trigger(e))
          );
          break;
        case RELATED:  // Only for Movies and Series
          hbox.getChildren().addAll(
            Buttons.create("Cast & Crew", e -> navigateToCastAndCrew(e, presentation.rootItem)),
            Buttons.create("Recommendations", e -> navigateToRecommendations(e))
          );
          if(presentation.rootItem.getData() instanceof Movie && ((Movie)presentation.rootItem.getData()).getCollectionDetails() != null) {
            hbox.getChildren().add(Buttons.create("Collection", e -> navigateToCollection(e)));
          }
          break;
        }
      }
    }

    private HBox createButtons() {
      HBox hbox = Containers.hbox("navigation-area");

      EventStreams.merge(EventStreams.invalidationsOf(presentation.buttonState), EventStreams.invalidationsOf(presentation.episodeItem))
        .conditionOnShowing(this)
        .withDefaultEvent(null)
        .subscribe(e -> updateButtons(hbox));

      return hbox;
    }

    private void navigateToCastAndCrew(ActionEvent event, MediaItem<?> mediaItem) {
      PresentationLoader.navigate(event, () -> castAndCrewPresentationFactory.create(mediaItem));
    }

    private void navigateToCollection(ActionEvent event) {
      @SuppressWarnings("unchecked")
      MediaItem<Movie> movieItem = (MediaItem<Movie>)presentation.rootItem;

      PresentationLoader.navigate(event, () -> productionCollectionPresentationFactory.create(movieItem.getData().getCollectionDetails().getIdentifier()));
    }

    private void navigateToRecommendations(ActionEvent event) {
      PresentationLoader.navigate(event, () -> recommendationsPresentationFactory.create(presentation.rootItem));
    }
  }

  private static String createSeasonEpisodeText(MediaItem<Episode> mediaItem) {
    int seasonNumber = mediaItem.getData().getSeasonNumber();

    return seasonNumber == 0 ? "Special"
        : seasonNumber == -1 ? "Extra"
                             : "Season " + seasonNumber + ", Episode " + mediaItem.getData().getNumber();
  }

  private static Button create(String title, String subtitle, EventHandler<ActionEvent> eventHandler) {
    Button button = Buttons.create("", eventHandler);

    VBox vbox = Containers.vbox("vbox", Labels.create(title, "title"));

    if(subtitle != null) {
      vbox.getChildren().add(Labels.create(subtitle, "subtitle"));
    }

    button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    button.setGraphic(vbox);

    return button;
  }

  private static StarRating createStarRating(Reception reception, double radius, double innerRadius) {
    StarRating starRating = new StarRating(radius, innerRadius, 5);

    if(reception != null) {
      starRating.setRating(reception.getRating() / 10);
    }
    else {
      starRating.setVisible(false);
      starRating.setManaged(false);
    }

    return starRating;
  }
}


