package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.library.scene.grid.ProductionCollectionFactory;
import hs.mediasystem.plugin.library.scene.grid.RecommendationsPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsPresentationFactory;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.runner.util.Dialogs;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.javafx.control.Buttons;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.MultiButton;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NavigationButtonsFactory {
  @Inject private ProductionCollectionFactory productionCollectionFactory;
  @Inject private RecommendationsPresentationFactory recommendationsPresentationFactory;
  @Inject private ContributionsPresentationFactory contributionsPresentationFactory;
  @Inject private PlaybackOverlayPresentation.TaskFactory factory;
  @Inject private WorkClient workClient;

  public HBox create(Work work, EventHandler<ActionEvent> showEpisodes) {
    HBox hbox = Containers.hbox("navigation-area");

    if(work.getType().isSerie()) {
      hbox.getChildren().add(new MultiButton(List.of(
        Buttons.create("Episodes", showEpisodes),
        createTrailerButton(work)
      )));
    }
    else if(work.getType().isPlayable()) {
      createPlayButton(work).ifPresent(hbox.getChildren()::add);
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

  private Optional<MultiButton> createPlayButton(Work work) {
    List<Button> nodes = new ArrayList<>();

    Duration resumePosition = work.getState().getResumePosition();

    if(work.getPrimaryStream().isPresent()) {
      if(resumePosition != null && !resumePosition.isZero()) {
        nodes.add(create(
          "Resume",
          "From " + SizeFormatter.SECONDS_AS_POSITION.format(resumePosition.toSeconds()),
          e -> PresentationLoader.navigate(e, factory.create(work, work.getPrimaryStream().orElseThrow().getAttributes().getUri(), resumePosition))
        ));
        nodes.add(create(
          "Play",
          "From start",
          e -> PresentationLoader.navigate(e, factory.create(work, work.getPrimaryStream().orElseThrow().getAttributes().getUri(), Duration.ZERO))
        ));
      }
      else {
        nodes.add(Buttons.create(
          "Play",
          e -> PresentationLoader.navigate(e, factory.create(work, work.getPrimaryStream().orElseThrow().getAttributes().getUri(), Duration.ZERO))
        ));
      }
    }

    if(!work.getType().isComponent()) {
      nodes.add(createTrailerButton(work));
    }

    return nodes.isEmpty() ? Optional.empty() : Optional.of(new MultiButton(nodes));
  }

  private Button createTrailerButton(Work work) {
    AtomicReference<VideoLink> trailerVideoLink = new AtomicReference<>();
    CompletableFuture.supplyAsync(() -> workClient.findVideoLinks(work.getId()))
      .thenAccept(videoLinks -> {
        videoLinks.stream().filter(vl -> vl.getType() == VideoLink.Type.TRAILER).findFirst().ifPresent(videoLink -> trailerVideoLink.set(videoLink));
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
      PresentationLoader.navigate(event, factory.create(work, URI.create("https://www.youtube.com/watch?v=" + videoLink.getKey()), Duration.ZERO));
    }
  }

  private MultiButton createRelatedButton(Work work) {
    List<Button> nodes = new ArrayList<>();

    nodes.add(Buttons.create("cast", "Cast & Crew", e -> navigateToCastAndCrew(e, work)));
    nodes.add(Buttons.create("recommended", "Recommended", e -> navigateToRecommendations(e, work)));

    work.getParent().filter(p -> p.getType().equals(MediaType.COLLECTION))
      .ifPresent(p -> {
        nodes.add(Buttons.create("collection", "Collection", e -> navigateToCollection(e, p.getId())));
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
