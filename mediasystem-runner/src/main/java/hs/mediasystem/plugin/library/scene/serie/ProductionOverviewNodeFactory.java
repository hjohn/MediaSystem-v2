package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.db.StreamStateProvider;
import hs.mediasystem.ext.basicmediatypes.Attribute;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.Serie;
import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaGridView;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory2;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation.State;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewPresentation;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.runner.LessLoader;
import hs.mediasystem.runner.SceneNavigator;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.AutoVerticalScrollPane;
import hs.mediasystem.util.javafx.BiasedImageView;
import hs.mediasystem.util.javafx.Buttons;
import hs.mediasystem.util.javafx.Containers;
import hs.mediasystem.util.javafx.Labels;
import hs.mediasystem.util.javafx.StarRating;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
  private static final String SYSTEM = "MediaSystem:Library:ProductionOverview";

  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private VideoDatabase videoDatabase;
  @Inject private SceneNavigator navigator;
  @Inject private Provider<CastAndCrewPresentation> castAndCrewPresentationProvider;
  @Inject private Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;
  @Inject private LocalMediaManager localMediaManager;
  @Inject private StreamStateProvider streamStateProvider;
  @Inject private SettingsStore settingsStore;

  @Override
  public Node create(ProductionPresentation presentation) {
    MediaItem<?> mediaItem = presentation.productionItem.get();
    AsyncImageProperty imageProperty = new AsyncImageProperty();

    imageProperty.imageHandleProperty().set(imageHandleFactory.fromURI(mediaItem.getProduction().getImage()));

    BiasedImageView poster = new BiasedImageView();

    poster.setOrientation(Orientation.VERTICAL);
    poster.imageProperty().bind(imageProperty);

    StarRating starRating = new StarRating(20, 8, 5);

    starRating.setRating(mediaItem.getProduction().getReception().getRating() / 10);

    VBox leftTitleBox = Containers.vbox(
      Labels.create("title", mediaItem.productionTitle),
      Labels.create("" + mediaItem.getProduction().getDate().getYear(), "release-year"),
      Labels.create(mediaItem.getProduction().getGenres().stream().collect(Collectors.joining(" / ")), "genres")
    );

    HBox titleBox = Containers.hbox(
      "title-panel",
      leftTitleBox,
      starRating
    );

    HBox.setHgrow(leftTitleBox, Priority.ALWAYS);

    VBox dynamicBox = Containers.vbox(
      "dynamic-panel"
    );

    VBox descriptionBox = new VBox(
      titleBox,
      dynamicBox
    );

    presentation.state.addListener(obs -> updateDynamicBox(presentation, dynamicBox));
    updateDynamicBox(presentation, dynamicBox);

    VBox.setVgrow(dynamicBox, Priority.ALWAYS);

    HBox hbox = new HBox(poster, descriptionBox);

    HBox.setHgrow(descriptionBox, Priority.ALWAYS);

    hbox.getStyleClass().add("main-panel");
    hbox.getStylesheets().add(LessLoader.compile(getClass().getResource("styles.less")).toExternalForm());

    return hbox;
  }

  void updateDynamicBox(ProductionPresentation presentation, VBox box) {
    State state = presentation.state.get();

    box.getChildren().clear();

    switch(state) {
    case OVERVIEW:
      {
        BorderPane borderPane = new BorderPane();

        box.getChildren().add(borderPane);

        AutoVerticalScrollPane descriptionScrollPane = new AutoVerticalScrollPane(
          Labels.create(presentation.productionItem.get().getProduction().getDescription(), "description"),
          12000,
          8
        );

        borderPane.setMinHeight(1);
        borderPane.setPrefHeight(1);
        borderPane.setCenter(descriptionScrollPane);

        HBox buttons = createButtons(presentation, presentation.productionItem.get());

        borderPane.setBottom(buttons);

        VBox.setVgrow(borderPane, Priority.ALWAYS);
      }
      break;
    case LIST:
      MediaGridView<MediaItem<Episode>> gridView = new MediaGridView<>();
      MediaGridViewCellFactory2<Episode> cellFactory = new MediaGridViewCellFactory2<>();

      gridView.visibleRows.set(1);
      gridView.visibleColumns.set(3);
      gridView.setOrientation(Orientation.HORIZONTAL);
      gridView.onItemSelected.set(e -> {
        presentation.episodeItem.set(e.getItem());
        presentation.state.set(State.EPISODE);
      });

      cellFactory.setTitleBindProvider(item -> item.productionTitle);
      cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
      cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
      cellFactory.setSequenceNumberExtractor(item -> Optional.ofNullable(((Episode)item.getData()).getNumber()).map(i -> "" + i).orElse(null));

      gridView.setCellFactory(cellFactory);
      gridView.setItems(getItems(presentation));

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

      //scrollPane.minHeightProperty().bind(seasonsBox.heightProperty());

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

      String settingKey = "last-selected:" + presentation.productionItem.get().getId();

      gridView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> settingsStore.storeSetting(SYSTEM, settingKey, current.getId()));

      String id = settingsStore.getSetting(SYSTEM, settingKey);

      gridView.getSelectionModel().select(0);

      if(id != null) {
        ObservableList<MediaItem<Episode>> items = gridView.getItems();

        for(int i = 0; i < items.size(); i++) {
          MediaItem<Episode> mediaItem = items.get(i);

          if(id.equals(mediaItem.getId())) {
            gridView.getSelectionModel().select(i);
            break;
          }
        }
      }

      box.getChildren().add(seasonsBox);
      box.getChildren().add(gridView);
      break;
    case EPISODE:
      AsyncImageProperty imageProperty = new AsyncImageProperty();
      MediaItem<Episode> mediaItem = (MediaItem<Episode>)presentation.episodeItem.get();

      imageProperty.imageHandleProperty().set(Optional.ofNullable(mediaItem.getProduction().getImage()).map(imageHandleFactory::fromURI).orElse(null));

      BiasedImageView poster = new BiasedImageView();

      poster.setOrientation(Orientation.VERTICAL);
      poster.imageProperty().bind(imageProperty);

      AutoVerticalScrollPane descriptionScrollPane = new AutoVerticalScrollPane(
        Labels.create(presentation.episodeItem.get().getProduction().getDescription(), "description"),
        12000,
        8
      );

      BorderPane borderPane = new BorderPane();
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
        Labels.create("title", mediaItem.productionTitle),
        Labels.create(createSeasonEpisodeText(mediaItem), "subtitle")
      );

      HBox.setHgrow(titleBox, Priority.ALWAYS);

      borderPane.setCenter(Containers.hbox(
        "episode-panel",
        Containers.vbox(
          Containers.hbox(
            titleBox,
            starRating
          ),
          descriptionScrollPane
        ),
        poster
      ));

      HBox buttons = createButtons(presentation, presentation.episodeItem.get());

      borderPane.setBottom(buttons);

      VBox.setVgrow(borderPane, Priority.ALWAYS);

      box.getChildren().add(borderPane);
      break;
    }
  }

  private static String createSeasonEpisodeText(MediaItem<Episode> mediaItem) {
    return mediaItem.getData().getSeasonNumber() == 0 ? "Special" : "Season " + mediaItem.getData().getSeasonNumber() + ", Episode " + mediaItem.getData().getNumber();
  }

  private HBox createButtons(ProductionPresentation presentation, MediaItem<?> mediaItem) {
    Production production = mediaItem.getProduction();

    Button trailerButton = new Button("Trailer");

    trailerButton.setDisable(true);

    CompletableFuture.supplyAsync(() -> videoDatabase.queryVideoLinks(production.getIdentifier()))
      .thenAccept(videoLinks -> {
        videoLinks.stream().filter(vl -> vl.getType() == VideoLink.Type.TRAILER).findFirst().ifPresent(videoLink -> {
          trailerButton.setDisable(false);
          trailerButton.setOnAction(event -> navigateToTrailer(event, videoLink, mediaItem));
        });
      });

    Button playButton = Buttons.create("Play", e -> navigateToPlay(e, mediaItem));

    playButton.setDisable(mediaItem.getStreams().isEmpty());

    return Containers.hbox(
      "navigation-area",
      mediaItem.getData() instanceof Serie ?
        Buttons.create("Episodes", e -> presentation.state.set(State.LIST)) :
        playButton,
      Buttons.create("Cast & Crew", e -> navigateToCastAndCrew(e, mediaItem)),
      trailerButton
    );
  }

  private void navigateToPlay(ActionEvent event, MediaItem<?> mediaItem) {
    navigator.navigateTo(playbackOverlayPresentationProvider.get().set(mediaItem, mediaItem.getStreams().iterator().next().getUri().toURI()));
    event.consume();
  }

  private void navigateToTrailer(ActionEvent event, VideoLink videoLink, MediaItem<?> mediaItem) {
    navigator.navigateTo(playbackOverlayPresentationProvider.get().set(mediaItem, URI.create("https://www.youtube.com/watch?v=" + videoLink.getKey())));
    event.consume();
  }

  private void navigateToCastAndCrew(ActionEvent event, MediaItem<?> mediaItem) {
    navigator.navigateTo(castAndCrewPresentationProvider.get().set(mediaItem));
    event.consume();
  }

  private ObservableList<MediaItem<Episode>> getItems(ProductionPresentation presentation) {
    @SuppressWarnings("unchecked")
    MediaItem<Serie> mediaItem = (MediaItem<Serie>)presentation.productionItem.get();
    Serie serieDescriptor = mediaItem.getData(); // fetchSerieDescriptor(productionItem);

    Map<Integer, Map<Integer, Set<MediaStream<?>>>> serieIndex = createSerieIndex(mediaItem);

    return FXCollections.observableArrayList(
      serieDescriptor.getSeasons().stream()
        .sorted((s1, s2) -> Integer.compare(s1.getNumber() == 0 ? Integer.MAX_VALUE : s1.getNumber(), s2.getNumber() == 0 ? Integer.MAX_VALUE : s2.getNumber()))
        .flatMap(s -> s.getEpisodes().stream())
        .map(s -> wrap(s, serieIndex)).collect(Collectors.toList()));
  }

  private Map<Integer, Map<Integer, Set<MediaStream<?>>>> createSerieIndex(MediaItem<?> serieItem) {
    Set<MediaStream<?>> episodeStreams = localMediaManager.findChildren(serieItem.getStreams().iterator().next().getUri());

    Map<Integer, Map<Integer, Set<MediaStream<?>>>> streamsByEpisodeBySeason = new HashMap<>();

    for(MediaStream<?> stream : episodeStreams) {
      String sequenceAttribute = (String)stream.getAttributes().get(Attribute.SEQUENCE);

      if(sequenceAttribute != null) {
        String[] parts = sequenceAttribute.split(",");

        if(parts.length == 2) {
          int seasonNumber = Integer.parseInt(parts[0]);
          String[] numbers = parts[1].split("-");

          for(int i = Integer.parseInt(numbers[0]); i <= Integer.parseInt(numbers[numbers.length - 1]); i++) {
            streamsByEpisodeBySeason.computeIfAbsent(seasonNumber, k -> new HashMap<>()).computeIfAbsent(i, k -> new HashSet<>()).add(stream);
          }
        }
        else {
          int episodeNumber = Integer.parseInt(parts[0]);

          streamsByEpisodeBySeason.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(episodeNumber, k -> new HashSet<>()).add(stream);
        }
      }
      else {
        streamsByEpisodeBySeason.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(0, k -> new HashSet<>()).add(stream);
      }
    }

    return streamsByEpisodeBySeason;
  }

  private MediaItem<Episode> wrap(Episode data, Map<Integer, Map<Integer, Set<MediaStream<?>>>> streamsByEpisodeBySeason) {
    Set<MediaStream<?>> streams = Optional.ofNullable(streamsByEpisodeBySeason.get(data.getSeasonNumber())).map(m -> m.get(data.getNumber())).orElse(Collections.emptySet());

    return new MediaItem<>(
      data,
      streams,
      countWatchedStreams(streams),
      streams.isEmpty() ? 0 : 1
    );
  }

  private int countWatchedStreams(Collection<MediaStream<?>> streams) {
    for(MediaStream<?> stream : streams) {
      if((boolean)streamStateProvider.get(stream.getStreamPrint()).getOrDefault("watched", false)) {
        return 1;
      }
    }

    return 0;
  }
}
