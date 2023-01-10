package hs.mediasystem.plugin.library.scene.overview;

import com.sun.javafx.binding.Subscription;

import hs.jfx.eventstream.core.Events;
import hs.jfx.eventstream.core.Invalidations;
import hs.mediasystem.plugin.cell.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaStatus;
import hs.mediasystem.plugin.library.scene.overview.EpisodePane.Model;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.ProductionPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.State;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.MediaStream;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.ui.transition.StandardTransitions;
import hs.mediasystem.util.javafx.ui.transition.TransitionPane;
import hs.mediasystem.util.javafx.ui.transition.multi.Scroll;
import hs.mediasystem.util.natural.SizeFormatter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProductionOverviewNodeFactory implements NodeFactory<ProductionPresentation> {
  private static final Logger LOGGER = Logger.getLogger(ProductionOverviewNodeFactory.class.getName());

  @Inject private ShowInfoEventHandler showInfoEventHandler;
  @Inject private NavigationButtonsFactory navigationButtonsFactory;
  @Inject private WorkClient workClient;

  @Override
  public Node create(ProductionPresentation presentation) {
    ProductionOverviewPane pane = new ProductionOverviewPane();

    ObservableValue<Boolean> showing = Nodes.showing(pane);

    pane.model.work.bind(presentation.root.when(showing));
    pane.model.missingFraction.bind(presentation.missingFraction.when(showing));
    pane.model.watchedFraction.bind(presentation.watchedFraction.when(showing));

    presentation.state
      .when(showing)
      .subscribe(state -> pane.model.dynamicPanel.set(() -> createDynamicBox(presentation)));

    presentation.showInfo
      .conditionOnShowing(pane)
      .subscribe(e -> showInfoEventHandler.handle(e, presentation.state.get() == State.OVERVIEW ? presentation.root.get() : presentation.selectedChild.getValue()));

    return pane;
  }

  private Pane createDynamicBox(ProductionPresentation presentation) {
    switch(presentation.state.get()) {
    case LIST:
      return buildEpisodeListUI(presentation);
    case EPISODE:
      return buildEpisodeDynamicUI(presentation);
    default:
      return buildOverviewUI(presentation);
    }
  }

  private Pane buildOverviewUI(ProductionPresentation presentation) {
    DetailAndCastPane pane = new DetailAndCastPane();

    pane.getStyleClass().add("overview-panel");

    ObservableValue<Details> details = presentation.root
      .when(Nodes.showing(pane))
      .map(Work::getDetails);

    pane.model.tagline.bind(
      details
        .map(Details::getTagline)
        .map(optTagline -> optTagline.map(t -> "“" + t + "”").orElse(null))
    );

    pane.model.description.bind(
      details
        .map(Details::getDescription)
        .map(optDesc -> optDesc.orElse(""))
    );

    BorderPane borderPane = new BorderPane();

    borderPane.setMinHeight(1);
    borderPane.setPrefHeight(1);
    borderPane.setCenter(pane);
    borderPane.getStyleClass().add("overview-dynamic-panel");

    presentation.root
      .when(Nodes.showing(pane))
      .subscribe(current -> {
        CompletableFuture.supplyAsync(() -> workClient.findContributions(current.getId()))
          .thenAcceptAsync(contributors -> pane.model.contributors.set(contributors), Platform::runLater)
          .whenComplete((v, e) -> {
            if(e != null) {
              LOGGER.log(Level.WARNING, "Unable to fill cast pane", e);
            }
          });

        borderPane.setBottom(
          Containers.stack(navigationButtonsFactory.create(current, e -> presentation.toListState()), buildStreamInfoPanel(current))
        );
      });

    VBox.setVgrow(borderPane, Priority.ALWAYS);

    return Containers.vbox("dynamic-panel", borderPane);
  }

  private static Pane buildStreamInfoPanel(Work current) {
    MediaStream stream = current.getPrimaryStream().orElse(null);
    int streamCount = current.getStreams().size();

    if(stream == null) {
      return null;
    }

    HBox timeBox = current.getStreams().stream()
      .map(MediaStream::state)
      .map(hs.mediasystem.ui.api.domain.State::lastConsumptionTime)
      .flatMap(Optional::stream)
      .max(Comparator.naturalOrder())
      .map(time -> Containers.hbox(
        "last-watched-box",
        Labels.create("last-watched-icon", "📷"),
        Labels.create("last-watched-text", SizeFormatter.formatTimeAgo(LocalDateTime.ofInstant(time, ZoneId.systemDefault())))
      ))
      .orElse(null);

    return Containers.vbox().style("stream-info-panel").mouseTransparent().nodes(
      stream.duration().map(d -> Containers.hbox(
        "duration-box",
        Labels.create("duration-icon", "🕑"),
        Labels.create("duration-text", SizeFormatter.SECONDS_AS_POSITION.format(d.toSeconds()))
      )).orElse(null),
      timeBox,
      Optional.ofNullable(streamCount > 1 ? streamCount : null).map(count -> Containers.hbox(
        "stream-count-box",
        Labels.create("stream-count-icon", "📁"),
        Labels.create("stream-count-text", streamCount + " copies")
      )).orElse(null)
    );
  }

  private Pane buildEpisodeListUI(ProductionPresentation presentation) {
    MediaGridViewCellFactory<Work> cellFactory = new MediaGridViewCellFactory<>((item, model) -> {
      model.title.set(item.getDetails().getTitle());
      model.annotation1.set(item.getDetails().getSequence().map(this::createSequenceInfo).orElse(null));
      model.imageHandle.set(item.getDetails().getSampleImage().orElse(null));
      model.status.set(item.getStreams().isEmpty() ? MediaStatus.UNAVAILABLE : item.getState().consumed() ? MediaStatus.WATCHED : MediaStatus.AVAILABLE);
    });

    cellFactory.setPlaceHolderAspectRatio(16.0 / 9.0);
    cellFactory.setMinRatio(4.0 / 3.0);

    EpisodeListPane pane = new EpisodeListPane(cellFactory);

    Invalidations.of(presentation.children)
      .withDefault(null)  // converts to value stream, so event fires each time it becomes visible
      .conditionOn(Nodes.showing(pane))
      .subscribe(i -> pane.model.setEpisodes(presentation.children.get()));

    Invalidations.of(presentation.selectedChild)
      .withDefault(null)  // converts to value stream, so event fires each time it becomes visible
      .conditionOn(Nodes.showing(pane))
      .subscribe(i -> pane.model.setSelected(presentation.selectedChild.get()));

    Events.of(pane.model.selected).conditionOn(Nodes.showing(pane)).subscribe(presentation.selectedChild::set);

    pane.model.onItemSelected.set(e -> presentation.toEpisodeState());

    pane.getStyleClass().add("dynamic-panel");

    return pane;
  }

  private Pane buildEpisodeDynamicUI(ProductionPresentation presentation) {
    BorderPane borderPane = new BorderPane() {
      Subscription childTransitionPanelSubscription;

      {
        ObjectProperty<Work> selectedChildCopy = new SimpleObjectProperty<>(presentation.selectedChild.get());  // Copy here so that a change in selected doesn't update a pane being scrolled out

        TransitionPane transitionPane = new TransitionPane(new Scroll(), buildEpisodeUI(selectedChildCopy));
        TransitionPane streamInfoPane = new TransitionPane(StandardTransitions.fade(250, 500));

        presentation.selectedChild
          .when(Nodes.showing(this))
          .addListener((obs, old, current) -> {
            List<Work> episodes = presentation.children.get();

            if(old.getId().equals(current.getId())) {  // item was just refreshed, no need to transition
              selectedChildCopy.set(current);
            }
            else {
              int oldIndex = episodes.indexOf(old);
              int newIndex = episodes.indexOf(current);

              /*
               * Each EpisodePane listens on selectedChildCopy in order to do a refresh if needed (when in a dialog Viewed
               * status is changed for example). As here we're switching to another episode (by scrolling the entire pane)
               * updating the selectedChildCopy would change the content of both the pane being scrolled out as well as
               * the one being scrolled in. This is why we unsubscribe the old pane's refresh subscription here.
               */

              childTransitionPanelSubscription.unsubscribe();
              selectedChildCopy.set(current);

              transitionPane.add(oldIndex > newIndex, buildEpisodeUI(selectedChildCopy));
            }

            setBottom(Containers.stack(navigationButtonsFactory.create(current, null), streamInfoPane));
            updateStreamInfoPane(streamInfoPane, current);
          });

        // duplicated
        setBottom(Containers.stack(navigationButtonsFactory.create(selectedChildCopy.get(), null), streamInfoPane));
        updateStreamInfoPane(streamInfoPane, selectedChildCopy.get());

        streamInfoPane.setMouseTransparent(true);

        setCenter(transitionPane);
        getProperties().put("presentation2", new EpisodePresentation(presentation.children, presentation.selectedChild));
        getStyleClass().add("episode-dynamic-panel");

        VBox.setVgrow(this, Priority.ALWAYS);
      }

      Pane buildEpisodeUI(ObjectProperty<Work> selectedChild) {
        EpisodePane pane = new EpisodePane();

        pane.getStyleClass().add("episode-panel");

        childTransitionPanelSubscription = selectedChild
          .when(Nodes.showing(pane))
          .subscribe(work -> {
            Details details = work.getDetails();

            double percentage = work.getState().consumed() ? 1.0 : work.getWatchedFraction().orElse(-1);

            Model model = pane.model;

            model.title.set(details.getTitle());
            model.description.set(details.getDescription().orElse(null));
            model.reception.set(details.getReception().orElse(null));
            model.releaseDate.set(details.getReleaseDate().orElse(null));
            model.sampleImage.set(details.getSampleImage().orElse(null));
            model.sequence.set(details.getSequence().orElseThrow());
            model.mediaStatus.set(percentage);
          });

        return pane;
      }

      void updateStreamInfoPane(TransitionPane streamInfoPane, Work work) {
        Pane panel = buildStreamInfoPanel(work);

        if(panel != null) {
          streamInfoPane.add(panel);
        }
      }
    };

    return Containers.vbox("dynamic-panel", borderPane);
  }

  private String createSequenceInfo(Sequence sequence) {
    if(sequence.type() == Type.SPECIAL) {
      return "Special " + sequence.number();
    }
    if(sequence.type() == Type.EXTRA) {
      return "Extra";
    }

    return "Ep. " + sequence.number();
  }
}
