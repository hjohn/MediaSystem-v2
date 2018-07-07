package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.Type;
import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.EntityLayout.Area;
import hs.mediasystem.plugin.library.scene.EntityPresentation;
import hs.mediasystem.plugin.library.scene.Layout;
import hs.mediasystem.plugin.library.scene.LibraryLocation;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.MediaItemFormatter;
import hs.mediasystem.plugin.library.scene.view.x.Fragment;
import hs.mediasystem.plugin.playback.scene.PlaybackLocation;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.runner.SceneNavigator;
import hs.mediasystem.util.javafx.AreaPane2;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.Binds;
import hs.mediasystem.util.javafx.GridPane;
import hs.mediasystem.util.javafx.GridPaneUtil;
import hs.mediasystem.util.javafx.ScaledImageView;
import hs.mediasystem.util.javafx.SpecialEffects;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DetailSetup implements Layout<EntityPresentation, DetailPresentation> {
  private static final Type MOVIE = Type.of("MOVIE");
  private static final Type EPISODE = Type.of("EPISODE");

  @Inject private VideoDatabase videoDatabase;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private SceneNavigator navigator;

  @Override
  public Class<?> getLocationClass() {
    return DetailLocation.class;
  }

  @Override
  public Fragment<DetailPresentation> createView(EntityPresentation entityPresentation) {
    DetailPresentation presentation = new DetailPresentation(entityPresentation);

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

        setupArea(Area.CENTER_TOP, gp, (p, n) -> p.at(3, 1).add(n));
        setupArea(Area.NAVIGATION, navigationArea);
        setupArea(Area.NAME, gp, (p, n) -> p.at(3, 5).align(HPos.CENTER).align(VPos.TOP).add(n));
        setupArea(Area.DETAILS, gp, (p, n) -> p.at(5, 1).add(n));
        setupArea(Area.CENTER, gp, (p, n) -> p.at(3, 1).spanning(1, 5).align(VPos.BOTTOM).add(n));
        setupArea(Area.CONTEXT_PANEL, leftOverlayPanel);
        setupArea(Area.PREVIEW_PANEL, rightOverlayPanel);
        setupArea(Area.INFORMATION_PANEL, gp, (p, n) -> p.at(1, 6).spanning(5, 1).align(VPos.BOTTOM).align(HPos.LEFT).styleClass("information-panel").add(n));
      }
    };

    presentation.location.addListener((obs, o, n) -> locationChanged(n, areaPane, presentation));
    presentation.location.bind(Binds.monadic(entityPresentation.location).filter(DetailLocation.class::isInstance).orElse(null));
    areaPane.setMinSize(1, 1);

    return new Fragment<>(areaPane, presentation);
  }

  private void locationChanged(LibraryLocation location, AreaPane2<Area> areaPane, DetailPresentation presentation) {
    if(location == null) {
      return;
    }

    MediaItem<?> mediaItem = (MediaItem<?>)location.getItem();
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

    Type type = production.getIdentifier().getDataSource().getType();

    areaPane.add(Area.NAME, new Label() {{
      getStyleClass().add("entity-title");
      setEffect(SpecialEffects.createNeonEffect(50));
      textProperty().set(production.getName());
    }});

    if(type.equals(MOVIE) || type.equals(EPISODE)) {
      areaPane.add(Area.NAVIGATION, new Button("Play") {{
        setOnAction(e -> navigateToPlay(e, mediaItem));
        setDisable(mediaItem.getStreams().isEmpty());
      }});
    }
    else {
      areaPane.add(Area.NAVIGATION, new Button("Episodes") {{
        setOnAction(e -> navigateToChildren(e, mediaItem));
      }});
    }
    areaPane.add(Area.NAVIGATION, new Button("Cast & Crew") {{
      setOnAction(e -> navigateToCastAndCrew(e, mediaItem));
    }});
    areaPane.add(Area.NAVIGATION, trailerButton);

    AsyncImageProperty poster = new AsyncImageProperty();

    poster.imageHandleProperty().set(Optional.ofNullable(production.getImage()).map(imageHandleFactory::fromURI).orElse(null));

    areaPane.add(Area.CENTER_TOP, new ScaledImageView() {{
      getStyleClass().add("poster-image");
      getStyleClass().add("glass-pane");
      imageProperty().bind(poster);
      setPreserveRatio(true);
      setSmooth(true);
      setAlignment(Pos.TOP_CENTER);
      setMinSize(1, 1);
    }});

//      gp.at(2, 5).add(new CastingsRow(Type.CAST, false) {{
//        castings.bind(media.castings);
//      }});

    areaPane.add(Area.DETAILS, new VBox() {{
      getStyleClass().add("glass-panes");
      getChildren().add(new VBox() {{
        getStyleClass().add("glass-pane");
        getChildren().add(new Label("RELEASE DATE") {{
          getStyleClass().add("field-label");
        }});
        getChildren().add(new Label() {{
          getStyleClass().addAll("field", "release-time");
          textProperty().set(MediaItemFormatter.formattedLocalDate(production.getDate()));
        }});
      }});

      getChildren().add(new VBox() {{
        getStyleClass().add("glass-pane");
        getChildren().add(new Label("PLOT") {{
          getStyleClass().add("field-label");
        }});
        getChildren().add(new Label() {{
          getStyleClass().addAll("field", "plot");
          textProperty().set(production.getDescription());
        }});
      }});
    }});

    presentation.getEntityPresentation().backdrop.unbind();
    presentation.getEntityPresentation().backdrop.set(Optional.ofNullable(production.getBackdrop()).map(imageHandleFactory::fromURI).orElse(null));    // TODO for episode, this is null, should use serie parent.. or the entire image as backdrop!?
  }

  private void navigateToPlay(ActionEvent event, MediaItem<?> mediaItem) {
    navigator.go(new PlaybackLocation(mediaItem, mediaItem.getStreams().iterator().next().getUri().toURI()));
    event.consume();
  }

  private void navigateToChildren(ActionEvent event, MediaItem<?> mediaItem) {
    navigator.go(new SerieSeasonsLocation(mediaItem));
    event.consume();
  }

  private void navigateToCastAndCrew(ActionEvent event, MediaItem<?> mediaItem) {
    navigator.go(new CastAndCrewLocation(mediaItem));
    event.consume();
  }

  private void navigateToTrailer(ActionEvent event, VideoLink videoLink, MediaItem<?> mediaItem) {
    // TODO should not supply main media, but trailer instead

    navigator.go(new PlaybackLocation(mediaItem, URI.create("https://www.youtube.com/watch?v=" + videoLink.getKey())));

    // Need to get /videos from TMDB or use appendToResponse feature, it contains a "key" which can be added to a YouTube URL "?v=<<key>>".  YouTube needs to played through API though, check if that works still.
    // Media testMedia = new YouTubeFeed.YouTubeVideo("https://www.youtube.com/watch?v=lQ1q_fTC-a8", "Prometheus Trailer");

    // Event.fireEvent(event.getTarget(), new LocationChangeEvent(new PlayerLocation(null, testMedia, 0)));

    event.consume();
  }
}
