package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.library.scene.grid.ProductionCollectionFactory;
import hs.mediasystem.plugin.library.scene.grid.RecommendationsPresentation;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentation.State;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.javafx.control.Buttons;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.MultiButton;

import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.EventStreams;

@Singleton
public class NavigationButtonsFactory {
  private static final MediaType SERIE = MediaType.of("SERIE");
  private static final MediaType COLLECTION = MediaType.of("COLLECTION");

  @Inject private ProductionCollectionFactory productionCollectionFactory;
  @Inject private RecommendationsPresentation.Factory recommendationsPresentationFactory;
  @Inject private ContributionsPresentation.Factory contributionsPresentationFactory;

  public HBox create(ProductionPresentation presentation) {
    HBox hbox = Containers.hbox("navigation-area");

    EventStreams.merge(EventStreams.invalidationsOf(presentation.episodeItem))
      .conditionOnShowing(hbox)
      .withDefaultEvent(null)
      .subscribe(e -> updateButtons(presentation, hbox));

    return hbox;
  }

  private void updateButtons(ProductionPresentation presentation, HBox hbox) {
    if(presentation.state.get() != State.LIST) {
      hbox.getChildren().clear();

      hbox.getChildren().addAll(
        presentation.rootItem.getType().equals(SERIE) && presentation.state.get() == State.OVERVIEW ?
          Buttons.create("Episodes", e -> presentation.toListState()) : createPlayButton(presentation),
        presentation.state.get() == State.OVERVIEW ?  // Only show Related for Movie and Serie, for Episode only Cast&Crew is available
          createRelatedButton(presentation) :
          Buttons.create("Cast & Crew", e -> navigateToCastAndCrew(e, presentation.episodeItem.getValue()))
      );
    }
  }

  private static MultiButton createPlayButton(ProductionPresentation presentation) {
    List<Button> nodes = new ArrayList<>();

    if(presentation.resume.enabledProperty().getValue()) {
      nodes.add(create("Resume", "From " + SizeFormatter.SECONDS_AS_POSITION.format(presentation.resume.resumePosition.getValue().toSeconds()), e -> presentation.resume.trigger(e)));
      nodes.add(create("Play", "From start", e -> presentation.play.trigger(e)));
    }
    else {
      nodes.add(Buttons.create(presentation.play));
    }

    if(presentation.state.get() == State.OVERVIEW) {
      nodes.add(Buttons.create(presentation.playTrailer));
    }

    return new MultiButton(nodes);
  }

  private MultiButton createRelatedButton(ProductionPresentation presentation) {
    List<Button> nodes = new ArrayList<>();

    nodes.add(Buttons.create("cast", "Cast & Crew", e -> navigateToCastAndCrew(e, presentation.rootItem)));
    nodes.add(Buttons.create("recommended", "Recommended", e -> navigateToRecommendations(e, presentation.rootItem)));

    presentation.rootItem.getParent().filter(p -> p.getType().equals(COLLECTION))
      .ifPresent(p -> {
        nodes.add(Buttons.create("collection", "Collection", e -> navigateToCollection(e, p.getId())));
      });

    return new MultiButton(nodes);
  }

  private void navigateToCastAndCrew(ActionEvent event, Work work) {
    PresentationLoader.navigate(event, () -> contributionsPresentationFactory.create(work));
  }

  private void navigateToCollection(ActionEvent event, WorkId id) {
    PresentationLoader.navigate(event, () -> productionCollectionFactory.create(id));
  }

  private void navigateToRecommendations(ActionEvent event, Work work) {
    PresentationLoader.navigate(event, () -> recommendationsPresentationFactory.create(work));
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
