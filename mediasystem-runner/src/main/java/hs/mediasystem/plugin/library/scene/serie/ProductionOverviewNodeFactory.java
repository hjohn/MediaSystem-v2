package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.plugin.library.scene.MediaGridView;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation.State;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewPresentation;
import hs.mediasystem.plugin.library.scene.view.RecommendationsPresentation;
import hs.mediasystem.plugin.library.scene.view.TransitionPane;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.runner.LessLoader;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.javafx.AsyncImageProperty2;
import hs.mediasystem.util.javafx.AutoVerticalScrollPane;
import hs.mediasystem.util.javafx.BiasedImageView;
import hs.mediasystem.util.javafx.Buttons;
import hs.mediasystem.util.javafx.Containers;
import hs.mediasystem.util.javafx.Labels;
import hs.mediasystem.util.javafx.StarRating;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.effect.Bloom;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class ProductionOverviewNodeFactory implements NodeFactory<ProductionPresentation> {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private Provider<CastAndCrewPresentation> castAndCrewPresentationProvider;
  @Inject private Provider<RecommendationsPresentation> recommendationsPresentationProvider;

  @Override
  public Node create(ProductionPresentation presentation) {
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> CREATING PRODUCTION OVERVIEW NODE");
    return new MainPanel(presentation);
  }

  private class MainPanel extends HBox {
    public MainPanel(ProductionPresentation presentation) {
      presentation.update();

      MediaItem<?> mediaItem = presentation.rootItem;
      AsyncImageProperty2 imageProperty = new AsyncImageProperty2();

      imageProperty.imageHandleProperty().set(Optional.ofNullable(mediaItem.getProduction().getImage()).map(imageHandleFactory::fromURI).orElse(null));

      BiasedImageView poster = new BiasedImageView();

      poster.setOrientation(Orientation.VERTICAL);
      poster.imageProperty().bind(imageProperty);

      StarRating starRating = new StarRating(20, 8, 5);

      if(mediaItem.getProduction().getReception() != null) {
        starRating.setRating(mediaItem.getProduction().getReception().getRating() / 10);
      }
      else {
        starRating.setVisible(false);
      }

      String year = "";

      if(mediaItem.getData() instanceof Serie) {
        Serie serie = (Serie)mediaItem.getData();

        if(serie.getDate() != null) {
          year = "" + serie.getDate().getYear();

          if(serie.getState() == Serie.State.ENDED && serie.getLastAirDate() != null && serie.getLastAirDate().getYear() != serie.getDate().getYear()) {
            year += " - " + serie.getLastAirDate().getYear();
          }
          else if(serie.getState() == Serie.State.CONTINUING) {
            year += " -";
          }
        }
      }
      else {
        year = Optional.ofNullable(mediaItem.getProduction().getDate()).map(LocalDate::getYear).map(Object::toString).orElse("");
      }

      VBox leftTitleBox = Containers.vbox(
        Labels.create("title", mediaItem.productionTitle),
        Labels.create(year, "release-year"),
        Labels.create(mediaItem.getProduction().getGenres().stream().collect(Collectors.joining(" / ")), "genres")
      );

      HBox titleBox = Containers.hbox(
        "title-panel",
        leftTitleBox,
        starRating
      );

      HBox.setHgrow(leftTitleBox, Priority.ALWAYS);

      TransitionPane dynamicBoxContainer = new TransitionPane(new TransitionPane.FadeIn(), createDynamicBox(presentation));
      Containers.vbox(
        "dynamic-panel"
      );

      VBox.setVgrow(dynamicBoxContainer, Priority.ALWAYS);

      VBox descriptionBox = new VBox(
        titleBox,
        dynamicBoxContainer
      );

      presentation.state.addListener(obs -> dynamicBoxContainer.add(createDynamicBox(presentation)));

      getChildren().addAll(poster, descriptionBox);

      HBox.setHgrow(descriptionBox, Priority.ALWAYS);

      getStyleClass().add("main-panel");
      getStylesheets().add(LessLoader.compile(getClass().getResource("styles.less")).toExternalForm());
    }

    VBox createDynamicBox(ProductionPresentation presentation) {
      VBox box = Containers.vbox("dynamic-panel");

      State state = presentation.state.get();
      MediaItem<?> productionItem = presentation.rootItem;

      box.getChildren().clear();

      switch(state) {
      case OVERVIEW:
        {
          BorderPane borderPane = new BorderPane();
          Production production = productionItem.getProduction();

          if(production instanceof Movie) {
            Movie movie = (Movie)production;

            if(movie.getTagLine() != null && !movie.getTagLine().isEmpty()) {
              box.getChildren().add(Labels.create("“" + movie.getTagLine() + "”", "tag-line"));
            }
          }

          box.getChildren().add(borderPane);

          AutoVerticalScrollPane descriptionScrollPane = new AutoVerticalScrollPane(
            Labels.create(production.getDescription(), "description"),
            12000,
            8
          );

          borderPane.setMinHeight(1);
          borderPane.setPrefHeight(1);
          borderPane.setCenter(descriptionScrollPane);

          HBox buttons = createButtons(presentation);

          borderPane.setBottom(buttons);

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

        cellFactory.setTitleBindProvider(item -> item.productionTitle);
        cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
        cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
        cellFactory.setSequenceNumberExtractor(item -> Optional.ofNullable(item.getData().getNumber()).map(i -> "" + i).orElse(null));
        cellFactory.setPlaceHolderAspectRatio(9.0 / 16.0);
        cellFactory.setMinRatio(4.0 / 3.0);

        gridView.setCellFactory(cellFactory);
        gridView.setItems(presentation.episodesPresentation.episodeItems);

        VBox.setVgrow(gridView, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane();

        scrollPane.getStyleClass().add("season-bar");

        scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollBarPolicy.NEVER);

        HBox seasonsBox = new SeasonBar();

        seasonsBox.getStyleClass().add("season-bar");

        Set<String> knownSeasons = new HashSet<>();
        List<Integer> jumpPoints = new ArrayList<>();

        seasonsBox.setMinWidth(1);
        seasonsBox.setPrefWidth(1);
        seasonsBox.getChildren().add(new Label("Season"));

        for(int i = 0; i < gridView.getItems().size(); i++) {
          MediaItem<Episode> episode = gridView.getItems().get(i);
          int seasonNumber = episode.getData().getSeasonNumber();
          String season = seasonNumber == 0 ? "Specials" : "" + seasonNumber;

          if(!knownSeasons.contains(season)) {
            jumpPoints.add(i);
            knownSeasons.add(season);

            Label label = new Label(season);

            label.setUserData(seasonNumber);

            seasonsBox.getChildren().add(label);
          }
        }

        gridView.jumpPoints.set(jumpPoints);

        gridView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> {
          int seasonNumber = current.getData().getSeasonNumber();

          for(Node node : seasonsBox.getChildren()) {
            node.getStyleClass().remove("focused");
            node.setEffect(null);

            if((node.getUserData() == null && seasonNumber != 0) || (node.getUserData() != null && (Integer)node.getUserData() == seasonNumber)) {
              node.getStyleClass().add("focused");
              node.setEffect(new Bloom(0.2));
            }
          }
        });

        box.getChildren().add(seasonsBox);
        box.getChildren().add(gridView);

        gridView.getSelectionModel().select(presentation.episodesPresentation.episodeItem.get());
        presentation.episodesPresentation.episodeItem.bind(gridView.getSelectionModel().selectedItemProperty());
        break;
      case EPISODE:
        buildEpisodeDynamicUI(presentation, box);
        break;
      }

      return box;
    }

    private void buildEpisodeDynamicUI(ProductionPresentation presentation, VBox box) {
      presentation.episodesPresentation.episodeItem.unbind();

      ObservableList<MediaItem<Episode>> episodes = presentation.episodesPresentation.episodeItems;
      TransitionPane transitionPane = new TransitionPane(new TransitionPane.Scroll(), buildEpisodeUI(presentation));
      BorderPane borderPane = new BorderPane();

      ChangeListener<? super MediaItem<Episode>> listener = (obs, old, current) -> {
        if(episodes.indexOf(old) > episodes.indexOf(current)) {
          transitionPane.add(0, buildEpisodeUI(presentation));
        }
        else {
          transitionPane.add(buildEpisodeUI(presentation));
        }
      };

      presentation.episodesPresentation.episodeItem.addListener(listener);

      box.sceneProperty().addListener((ov, old, current) -> {
        if(current == null) {
          presentation.episodesPresentation.episodeItem.removeListener(listener);
        }
      });

      borderPane.setCenter(transitionPane);

      HBox buttons = createButtons(presentation);

      borderPane.setBottom(buttons);

      VBox.setVgrow(borderPane, Priority.ALWAYS);

      borderPane.getProperties().put("presentation2", new EpisodePresentation(presentation.episodesPresentation));

      box.getChildren().add(borderPane);
    }

    private HBox buildEpisodeUI(ProductionPresentation presentation) {
      MediaItem<Episode> mediaItem = presentation.episodesPresentation.episodeItem.get();
      AsyncImageProperty2 imageProperty = new AsyncImageProperty2();

      imageProperty.imageHandleProperty().set(Optional.ofNullable(mediaItem.getProduction().getImage()).map(imageHandleFactory::fromURI).orElse(null));

      BiasedImageView poster = new BiasedImageView();

      poster.setOrientation(Orientation.VERTICAL);
      poster.imageProperty().bind(imageProperty);

      AutoVerticalScrollPane descriptionScrollPane = new AutoVerticalScrollPane(
        Labels.create(presentation.episodesPresentation.episodeItem.get().getProduction().getDescription(), "description"),
        12000,
        8
      );

      StarRating starRating = new StarRating(13, 5, 5);
      Reception reception = mediaItem.getProduction().getReception();

      if(reception != null) {
        starRating.setRating(reception.getRating() / 10);
      }
      else {
        starRating.setVisible(false);
        starRating.setManaged(false);
      }

      VBox titleBox = Containers.vbox(
        Labels.create("title", presentation.episodesPresentation.episodeItem.get().productionTitle),
        Labels.create(createSeasonEpisodeText(mediaItem), "subtitle")
      );

      HBox.setHgrow(titleBox, Priority.ALWAYS);

      return Containers.hbox(
        "episode-panel",
        Containers.vbox(
          Containers.hbox(
            titleBox,
            starRating
          ),
          descriptionScrollPane
        ),
        poster
      );
    }

    private void updateButtons(HBox hbox, ProductionPresentation presentation) {
      if(presentation.state.get() != State.LIST) {
        MediaItem<?> mediaItem = presentation.episodeOrMovieItem.getValue() == null ? presentation.rootItem : presentation.episodeOrMovieItem.getValue();

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
              Buttons.create("Cast & Crew", e -> navigateToCastAndCrew(e, mediaItem)),
            Buttons.create(presentation.playTrailer)
          );
          break;
        case PLAY_RESUME:
          hbox.getChildren().addAll(
            create("Play", "From start", e -> presentation.play.trigger(e)),
            create("Resume", "From " + SizeFormatter.SECONDS_AS_POSITION.format(presentation.resume.resumePosition.getValue()), e -> presentation.resume.trigger(e))
          );
          break;
        case RELATED:  // Only for Movies and Series
          hbox.getChildren().addAll(
            Buttons.create("Cast & Crew", e -> navigateToCastAndCrew(e, presentation.rootItem)),
            Buttons.create("Recommendations", e -> navigateToRecommendations(e, presentation.rootItem))
          );
          break;
        }
      }
    }

    private HBox createButtons(ProductionPresentation presentation) {
      HBox hbox = Containers.hbox("navigation-area");

      ChangeListener<Object> listener = (ov, old, current) -> updateButtons(hbox, presentation);

      System.out.println("&&& Adding listener: " + listener);
      presentation.buttonState.addListener(listener);
      presentation.episodeOrMovieItem.addListener(listener);

      hbox.sceneProperty().addListener((ov, old, current) -> {
        if(current == null) {
          System.out.println("&&& Removing listener: " + listener);
          presentation.buttonState.removeListener(listener);
          presentation.episodeOrMovieItem.removeListener(listener);
        }
      });

      updateButtons(hbox, presentation);

      return hbox;
    }

    private void navigateToCastAndCrew(ActionEvent event, MediaItem<?> mediaItem) {
      Event.fireEvent(event.getTarget(), NavigateEvent.to(castAndCrewPresentationProvider.get().set(mediaItem)));
      event.consume();
    }

    private void navigateToRecommendations(ActionEvent event, MediaItem<?> mediaItem) {
      Event.fireEvent(event.getTarget(), NavigateEvent.to(recommendationsPresentationProvider.get().set(mediaItem)));
      event.consume();
    }
  }

  private static String createSeasonEpisodeText(MediaItem<Episode> mediaItem) {
    return mediaItem.getData().getSeasonNumber() == 0 ? "Special" : "Season " + mediaItem.getData().getSeasonNumber() + ", Episode " + mediaItem.getData().getNumber();
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
}
