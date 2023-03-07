package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.domain.media.MediaStructure;
import hs.mediasystem.domain.media.Resolution;
import hs.mediasystem.domain.media.Snapshot;
import hs.mediasystem.domain.media.VideoTrack;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.library.scene.grid.ProductionCollectionFactory;
import hs.mediasystem.plugin.library.scene.grid.RecommendationsPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsPresentationFactory;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.runner.dialog.DialogPane;
import hs.mediasystem.runner.dialog.Dialogs;
import hs.mediasystem.runner.presentation.PresentationLoader;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.MediaStream;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.image.ImageHandleFactory;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Buttons;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.MultiButton;
import hs.mediasystem.util.natural.SizeFormatter;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NavigationButtonsFactory {
  private static final String STYLES_URL = LessLoader.compile(NavigationButtonsFactory.class, "play-dialog.less");
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
  private static final Comparator<MediaStream> LAST_WATCHED = Comparator.comparing((MediaStream s) -> s.state().lastConsumptionTime().orElse(Instant.MIN)).reversed();

  private static final Function<MediaStream, Resolution> RESOLUTION_EXTRACTOR =
    ms -> ms.mediaStructure().stream()
      .map(MediaStructure::videoTracks)
      .flatMap(List::stream)
      .findFirst()
      .map(VideoTrack::resolution)
      .orElse(null);

  private static final Comparator<MediaStream> HIGHEST_RESOLUTION_AND_LATEST_FIRST =
    Comparator.comparing(RESOLUTION_EXTRACTOR, Comparator.nullsLast(Comparator.naturalOrder()))
      .thenComparing(MediaStream::lastModificationTime)
      .reversed();

  @Inject private ProductionCollectionFactory productionCollectionFactory;
  @Inject private RecommendationsPresentationFactory recommendationsPresentationFactory;
  @Inject private ContributionsPresentationFactory contributionsPresentationFactory;
  @Inject private PlaybackOverlayPresentation.TaskFactory factory;
  @Inject private WorkClient workClient;
  @Inject private ImageHandleFactory imageHandleFactory;

  public HBox create(Work work, EventHandler<ActionEvent> showEpisodes) {
    HBox hbox = Containers.hbox("navigation-area");

    if(work.getType().isSerie()) {
      hbox.getChildren().add(new MultiButton(List.of(
        Buttons.create("Episodes", showEpisodes),
        createTrailerButton(work)
      )));
    }
    else if(work.getType().isPlayable()) {
      createPlayButtons(work).ifPresent(hbox.getChildren()::add);
    }

    if(work.getId().getDataSource().getName().equals("TMDB")) {
      if(work.getType().isComponent() && work.getType().isPlayable()) {
        hbox.getChildren().add(Buttons.create("Cast & Crew", e -> navigateToCastAndCrew(e, work)));
      }
      else {
        hbox.getChildren().add(createRelatedButton(work));
      }
    }

    return hbox;
  }

  private Optional<MultiButton> createPlayButtons(Work work) {
    List<Button> nodes = new ArrayList<>();

    if(work.getPrimaryStream().isPresent()) {
      List<MediaStream> resumableStreams = work.getStreams().stream()
        .filter(s -> s.state().resumePosition() != null && !s.state().resumePosition().isZero())
        .sorted(LAST_WATCHED)
        .collect(Collectors.toList());

      // Create Resume Button:
      if(resumableStreams.size() > 1) {
        nodes.add(createIndirectPlayButton(work, resumableStreams, true));
      }
      else if(resumableStreams.size() == 1) {
        MediaStream stream = resumableStreams.get(0);
        Duration resumePosition = stream.state().resumePosition();

        nodes.add(createDirectPlayButton(
          work,
          stream,
          "Resume",
          "From " + SizeFormatter.SECONDS_AS_POSITION.format(resumePosition.toSeconds()),
          resumePosition
        ));
      }

      // Create Play Button:
      if(work.getStreams().size() > 1) {
        nodes.add(createIndirectPlayButton(work, work.getStreams(), false));
      }
      else {
        String subtitle = resumableStreams.isEmpty() ? null : "From start";

        nodes.add(createDirectPlayButton(work, work.getStreams().get(0), "Play", subtitle, Duration.ZERO));
      }
    }

    if(!work.getType().isComponent()) {
      nodes.add(createTrailerButton(work));
    }

    return nodes.isEmpty() ? Optional.empty() : Optional.of(new MultiButton(nodes));
  }


  private Button createDirectPlayButton(Work work, MediaStream stream, String title, String subtitle, Duration startPosition) {
    return create(
      title,
      subtitle,
      e -> PresentationLoader.navigate(e, factory.create(work, stream, startPosition))
    );
  }

  private Button createIndirectPlayButton(Work work, List<MediaStream> streams, boolean resume) {
    return Buttons.create(
      resume ? "Resume..." : "Play...",
      e -> {
        DialogPane<MediaStream> dialogPane = new DialogPane<>();

        dialogPane.getChildren().add(createStreamButtons(streams, dialogPane));

        Optional.ofNullable(dialogPane.showDialog(((Node)e.getTarget()).getScene(), true))
          .ifPresent(ms -> PresentationLoader.navigate(e, factory.create(work, ms, resume ? ms.state().resumePosition() : Duration.ZERO)));
      }
    );
  }

  private VBox createStreamButtons(List<MediaStream> streams, DialogPane<MediaStream> dialogPane) {
    VBox vbox = Containers.vbox().style("button-box").nodes(streams.stream()
      .sorted(HIGHEST_RESOLUTION_AND_LATEST_FIRST)
      .map(s -> streamToTitleButton(s, e -> dialogPane.close(s)))
      .collect(Collectors.toList())
    );

    vbox.setFillWidth(true);
    vbox.getStylesheets().add(STYLES_URL);

    return vbox;
  }

  private Node streamToTitleButton(MediaStream stream, EventHandler<ActionEvent> eventHandler) {
    String groupTitle = "";  // For "cut" information if we ever get it
    String title =
      Stream.of(
        stream.size().map(s -> SizeFormatter.BYTES_THREE_SIGNIFICANT.format(s)).orElse(null),
        DATE_TIME_FORMATTER.format(stream.lastModificationTime())
      )
      .filter(Objects::nonNull)
      .collect(Collectors.joining(" • "));

    String info =
      Stream.of(
        stream.duration().map(d -> SizeFormatter.SECONDS_AS_POSITION.format(d.toSeconds())).orElse(null),
        stream.mediaStructure().flatMap(ms -> ms.videoTracks().stream().findFirst()).map(VideoTrack::resolution).map(r -> r.width() + "✕" + r.height()).orElse(null)
      )
      .filter(Objects::nonNull)
      .collect(Collectors.joining(" • "));

    Button button = Buttons.create("stream-button", null, eventHandler);

    VBox descriptionBox = Containers.vbox("description-box", Labels.create("group-title", groupTitle, Labels.HIDE_IF_EMPTY), Labels.create("title", title), Labels.create("info", info, Labels.HIDE_IF_EMPTY));
    HBox snapshotsBox = createSnapshotsBox(stream);

    snapshotsBox.setMaxWidth(Region.USE_PREF_SIZE);
    button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    HBox hbox = Containers.hbox("button-content-box", snapshotsBox, descriptionBox);

    button.setGraphic(hbox);  // extra container wrapped as it seems graphic kills custom styleclass

    return button;
  }

  private HBox createSnapshotsBox(MediaStream stream) {
    HBox box = Containers.hbox("snapshots-box");
    List<ImageHandle> handles = stream.snapshots().stream()
      .map(Snapshot::imageUri)
      .map(imageHandleFactory::fromURI)
      .limit(3)
      .collect(Collectors.toList());

    for(int i = 0; i < handles.size(); i++) {
      ImageHandle handle = handles.get(i);
      BiasedImageView imageView = new BiasedImageView();
      AsyncImageProperty property = new AsyncImageProperty(300, 200);

      imageView.setOrientation(Orientation.VERTICAL);
      imageView.imageProperty().bind(property);

      property.imageHandleProperty().set(handle);

      box.getChildren().add(imageView);
    }

    return box;
  }

  private Button createTrailerButton(Work work) {
    AtomicReference<VideoLink> trailerVideoLink = new AtomicReference<>();
    CompletableFuture.supplyAsync(() -> workClient.findVideoLinks(work.getId()))
      .thenAccept(videoLinks -> {
        videoLinks.stream().filter(vl -> vl.type() == VideoLink.Type.TRAILER).findFirst().ifPresent(videoLink -> trailerVideoLink.set(videoLink));
      });

    return Buttons.create(
      "Trailer",
      e -> playTrailer(e, work, trailerVideoLink)
    );
  }

  private void playTrailer(Event event, Work work, AtomicReference<VideoLink> trailerVideoLink) {
    VideoLink videoLink = trailerVideoLink.get();

    if(videoLink == null) {
      Dialogs.show(event, Labels.create("description", "No trailer available"));
    }
    else {
      PresentationLoader.navigate(event, factory.create(work, URI.create("https://www.youtube.com/watch?v=" + videoLink.key()), Duration.ZERO));
    }
  }

  private MultiButton createRelatedButton(Work work) {
    List<Button> nodes = new ArrayList<>();

    nodes.add(Buttons.create("cast", "Cast & Crew", e -> navigateToCastAndCrew(e, work)));
    nodes.add(Buttons.create("recommended", "Recommended", e -> navigateToRecommendations(e, work)));

    work.getParent().filter(p -> p.type().equals(MediaType.COLLECTION))
      .ifPresent(p -> {
        nodes.add(Buttons.create("collection", "Collection", e -> navigateToCollection(e, p.id())));
      });

    return new MultiButton(nodes);
  }

  private void navigateToCastAndCrew(ActionEvent event, Work work) {
    PresentationLoader.navigate(event, () -> contributionsPresentationFactory.create(work.getId()));
  }

  private void navigateToCollection(ActionEvent event, WorkId id) {
    PresentationLoader.navigate(event, () -> productionCollectionFactory.create(id));
  }

  private void navigateToRecommendations(ActionEvent event, Work work) {
    PresentationLoader.navigate(event, () -> recommendationsPresentationFactory.create(work.getId()));
  }

  private static Button create(String title, String subtitle, EventHandler<ActionEvent> eventHandler) {
    Button button = Buttons.create("", eventHandler);

    VBox vbox = Containers.vbox("vbox", Labels.create("title", title));

    if(subtitle != null) {
      vbox.getChildren().add(Labels.create("subtitle", subtitle));
    }

    button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    button.setGraphic(vbox);

    return button;
  }
}
