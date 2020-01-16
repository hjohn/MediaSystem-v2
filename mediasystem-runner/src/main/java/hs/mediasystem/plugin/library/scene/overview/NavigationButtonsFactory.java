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

    EventStreams.merge(EventStreams.invalidationsOf(presentation.buttonState), EventStreams.invalidationsOf(presentation.episodeItem))
      .conditionOnShowing(hbox)
      .withDefaultEvent(null)
      .subscribe(e -> updateButtons(presentation, hbox));

    return hbox;
  }

  private void updateButtons(ProductionPresentation presentation, HBox hbox) {
    if(presentation.state.get() != State.LIST) {
      hbox.getChildren().clear();

      switch(presentation.buttonState.get()) {
      case MAIN:
        hbox.getChildren().addAll(
          presentation.rootItem.getType().equals(SERIE) && presentation.state.get() == State.OVERVIEW ?
            Buttons.create("Episodes", e -> presentation.toListState()) :
              presentation.resume.enabledProperty().getValue() ?
                Buttons.create("Play", e -> presentation.toPlayResumeButtonState()) :
                Buttons.create(presentation.play),
          presentation.state.get() == State.OVERVIEW ?  // Only show Related for Movie and Serie, for Episode only Cast&Crew is available
            Buttons.create("Related", e -> presentation.toRelatedButtonState()) :
            Buttons.create("Cast & Crew", e -> navigateToCastAndCrew(e, presentation.episodeItem.getValue()))
        );
        if(presentation.state.get() == State.OVERVIEW) {
          hbox.getChildren().add(Buttons.create(presentation.playTrailer));
        }
        break;
      case PLAY_RESUME:
        hbox.getChildren().addAll(
          create("Resume", "From " + SizeFormatter.SECONDS_AS_POSITION.format(presentation.resume.resumePosition.getValue().toSeconds()), e -> presentation.resume.trigger(e)),
          create("Play", "From start", e -> presentation.play.trigger(e))
        );
        break;
      case RELATED:  // Only for Movies and Series
        hbox.getChildren().addAll(
          Buttons.create("Cast & Crew", e -> navigateToCastAndCrew(e, presentation.rootItem)),
          Buttons.create("Recommendations", e -> navigateToRecommendations(e, presentation.rootItem))
        );

        presentation.rootItem.getParent().filter(p -> p.getType().equals(COLLECTION))
          .ifPresent(p -> {
            hbox.getChildren().add(Buttons.create("Collection", e -> navigateToCollection(e, p.getId())));
          });
        break;
      }
    }
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
