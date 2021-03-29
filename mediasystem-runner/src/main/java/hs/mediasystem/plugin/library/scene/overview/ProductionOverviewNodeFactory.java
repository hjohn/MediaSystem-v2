package hs.mediasystem.plugin.library.scene.overview;

import hs.jfx.eventstream.Changes;
import hs.jfx.eventstream.Invalidations;
import hs.jfx.eventstream.Subscription;
import hs.jfx.eventstream.ValueStream;
import hs.jfx.eventstream.Values;
import hs.mediasystem.plugin.cell.ModelMediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.BinderProvider;
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
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.javafx.Nodes;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.transition.StandardTransitions;
import hs.mediasystem.util.javafx.control.transition.TransitionPane;
import hs.mediasystem.util.javafx.control.transition.multi.Scroll;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
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
  @Inject private BinderProvider binderProvider;
  @Inject private WorkClient workClient;

  @Override
  public Node create(ProductionPresentation presentation) {
    ProductionOverviewPane pane = new ProductionOverviewPane();

    Binding<Boolean> showing = Nodes.showing(pane);

    Values.of(presentation.root).conditionOn(showing).subscribe(pane.model.work::set);
    Values.of(presentation.missingFraction).conditionOn(showing).subscribe(pane.model.missingFraction::setValue);
    Values.of(presentation.watchedFraction).conditionOn(showing).subscribe(pane.model.watchedFraction::setValue);

    SharedModel mainPanel = new SharedModel(presentation);

    Values.of(presentation.state)
      .conditionOn(showing)
      .subscribe(state -> pane.model.dynamicPanel.set(() -> mainPanel.createDynamicBox()));

    presentation.showInfo
      .conditionOnShowing(pane)
      .subscribe(e -> showInfoEventHandler.handle(e, presentation.state.get() == State.OVERVIEW ? presentation.root.get() : presentation.selectedChild.getValue()));

    return pane;
  }

  private class SharedModel {
    private final ProductionPresentation presentation;
    private final ObjectProperty<Work> selectedChildCopy = new SimpleObjectProperty<>();

    private Subscription childTransitionPanelSubscription;

    public SharedModel(ProductionPresentation presentation) {
      this.presentation = presentation;
    }

    private Pane createDynamicBox() {
      switch(presentation.state.get()) {
      case LIST:
        return buildEpisodeListUI();
      case EPISODE:
        return buildEpisodeDynamicUI();
      default:
        return buildOverviewUI();
      }
    }

    private Pane buildOverviewUI() {
      DetailAndCastPane pane = new DetailAndCastPane();

      pane.getStyleClass().add("overview-panel");

      ValueStream<Details> detailsStream = Values.of(presentation.root)
        .conditionOn(Nodes.showing(pane))
        .map(Work::getDetails);

      detailsStream
        .map(Details::getTagline)
        .map(optTagline -> optTagline.map(t -> "“" + t + "”").orElse(null))
        .subscribe(pane.model.tagline::set);

      detailsStream
        .map(Details::getDescription)
        .map(optDesc -> optDesc.orElse(""))
        .subscribe(pane.model.description::set);

      BorderPane borderPane = new BorderPane();

      borderPane.setMinHeight(1);
      borderPane.setPrefHeight(1);
      borderPane.setCenter(pane);
      borderPane.getStyleClass().add("overview-dynamic-panel");

      Values.of(presentation.root)
        .conditionOn(Nodes.showing(pane))
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

    private Pane buildStreamInfoPanel(Work current) {
      MediaStream stream = current.getPrimaryStream().orElse(null);
      int streamCount = current.getStreams().size();

      if(stream == null) {
        return null;
      }

      return Containers.vbox(
        "stream-info-panel",
        Arrays.asList(
          stream.getDuration().map(d -> Containers.hbox(
            "duration-box",
            Labels.create("duration-icon", "🕑"),
            Labels.create("duration-text", SizeFormatter.SECONDS_AS_POSITION.format(d.toSeconds()))
          )).orElse(null),
          stream.getState().getLastConsumptionTime().map(time -> Containers.hbox(
            "last-watched-box",
            Labels.create("last-watched-icon", "📷"),
            Labels.create("last-watched-text", SizeFormatter.formatTimeAgo(LocalDateTime.ofInstant(time, ZoneId.systemDefault())))
          )).orElse(null),
          Optional.ofNullable(streamCount > 1 ? streamCount : null).map(count -> Containers.hbox(
            "stream-count-box",
            Labels.create("stream-count-icon", "📁"),
            Labels.create("stream-count-text", streamCount + " copies")
          )).orElse(null)
        ),
        Containers.MOUSE_TRANSPARENT
      );
    }

    private Pane buildEpisodeListUI() {
      ModelMediaGridViewCellFactory<Work> cellFactory = new ModelMediaGridViewCellFactory<>((item, model) -> {
        model.title.set(item.getDetails().getTitle());
        model.annotation1.set(item.getDetails().getSequence().map(this::createSequenceInfo).orElse(null));
        model.imageHandle.set(item.getDetails().getSampleImage().orElse(null));
        model.status.set(item.getStreams().isEmpty() ? MediaStatus.UNAVAILABLE : item.getState().isConsumed() ? MediaStatus.WATCHED : MediaStatus.AVAILABLE);
      });

      cellFactory.setPlaceHolderAspectRatio(16.0 / 9.0);
      cellFactory.setMinRatio(4.0 / 3.0);

      EpisodeListPane pane = new EpisodeListPane(cellFactory);

      Invalidations.of(presentation.children, presentation.selectedChild)
        .transactional()
        .withDefault()  // converts to value stream, so event fires each time it becomes visible
        .conditionOn(Nodes.showing(pane))
        .subscribe(i -> pane.model.setEpisodesAndSelected(presentation.children.get(), presentation.selectedChild.get()));

      Values.of(pane.model.selected)
        .conditionOn(Nodes.showing(pane))
        .subscribe(presentation.selectedChild::set);

      pane.model.onItemSelected.set(e -> presentation.toEpisodeState());

      pane.getStyleClass().add("dynamic-panel");

      return pane;
    }

    private Pane buildEpisodeDynamicUI() {
      selectedChildCopy.set(presentation.selectedChild.get());  // Copy here so that a change in selected doesn't update a pane being scrolled out

      TransitionPane transitionPane = new TransitionPane(new Scroll(), buildEpisodeUI());
      TransitionPane streamInfoPane = new TransitionPane(StandardTransitions.fade(250, 500));
      BorderPane borderPane = new BorderPane();

      Changes.diff(presentation.selectedChild)
        .conditionOn(Nodes.showing(borderPane))
        .subscribe(c -> {
          List<Work> episodes = presentation.children.get();

          if(c.getOldValue().getId().equals(c.getValue().getId())) {  // item was just refreshed, no need to transition
            selectedChildCopy.set(c.getValue());
          }
          else {
            int oldIndex = episodes.indexOf(c.getOldValue());
            int newIndex = episodes.indexOf(c.getValue());

            childTransitionPanelSubscription.unsubscribe();  // TODO this is somewhat confusing, as buildEpisodeUI assigns it again
            selectedChildCopy.set(c.getValue());

            transitionPane.add(oldIndex > newIndex, buildEpisodeUI());
          }

          borderPane.setBottom(Containers.stack(navigationButtonsFactory.create(c.getValue(), null), streamInfoPane));
          updateStreamInfoPane(streamInfoPane, c.getValue());
        });

      // duplicated
      borderPane.setBottom(Containers.stack(navigationButtonsFactory.create(selectedChildCopy.get(), null), streamInfoPane));
      updateStreamInfoPane(streamInfoPane, selectedChildCopy.get());

      streamInfoPane.setMouseTransparent(true);

      borderPane.setCenter(transitionPane);
      borderPane.getProperties().put("presentation2", new EpisodePresentation(presentation.children, presentation.selectedChild));
      borderPane.getStyleClass().add("episode-dynamic-panel");

      VBox.setVgrow(borderPane, Priority.ALWAYS);

      return Containers.vbox("dynamic-panel", borderPane);
    }

    private void updateStreamInfoPane(TransitionPane streamInfoPane, Work work) {
      Pane panel = buildStreamInfoPanel(work);

      if(panel != null) {
        streamInfoPane.add(panel);
      }
    }

    private String createSequenceInfo(Sequence sequence) {
      if(sequence.getType() == Type.SPECIAL) {
        return "Special " + sequence.getNumber();
      }
      if(sequence.getType() == Type.EXTRA) {
        return "Extra " + sequence.getNumber();
      }

      return "Ep. " + sequence.getNumber();
    }

    private Pane buildEpisodeUI() {
      EpisodePane pane = new EpisodePane();

      pane.getStyleClass().add("episode-panel");

      childTransitionPanelSubscription = Values.of(selectedChildCopy)
        .conditionOn(Nodes.showing(pane))
        .subscribe(work -> {
          Details details = work.getDetails();

          double percentage = work.getState().isConsumed() ? 1.0 : work.getWatchedFraction().orElse(-1);

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
  }
}
