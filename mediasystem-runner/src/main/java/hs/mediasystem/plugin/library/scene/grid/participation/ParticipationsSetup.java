package hs.mediasystem.plugin.library.scene.grid.participation;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.cell.MediaGridViewCellFactory.Model;
import hs.mediasystem.plugin.library.scene.MediaStatus;
import hs.mediasystem.plugin.library.scene.base.ContextLayout;
import hs.mediasystem.plugin.library.scene.grid.AbstractSetup;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsPresentationFactory.ParticipationsPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.ui.api.domain.Parent;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.javafx.base.ItemSelectedEvent;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ParticipationsSetup extends AbstractSetup<ConsolidatedParticipation, ConsolidatedParticipation, ParticipationsPresentation> {
  @Inject private ContextLayout contextLayout;
  @Inject private ProductionPresentationFactory productionPresentationFactory;

  @Override
  protected Node createContextPanel(ParticipationsPresentation presentation) {
    return contextLayout.create(presentation.person.get());
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<ConsolidatedParticipation> event, ParticipationsPresentation presentation) {
    PresentationLoader.navigate(event, () -> productionPresentationFactory.create(event.getItem().getWork().getId()));
  }

  @Override
  protected Node createPreviewPanel(ConsolidatedParticipation item) {
    return contextLayout.create(item.getWork());
  }

  @Override
  protected void fillModel(ConsolidatedParticipation item, Model model) {
    Work work = item.getWork();

    model.title.set(work.getDetails().getTitle());
    model.subtitle.set(item.getParticipationText());
    model.imageHandle.set(work.getDetails().getAnyCover().orElse(null));
    model.annotation1.set(work.getDetails().getYearRange());
    model.annotation2.set(work.getParent()
      .filter(p -> p.type().equals(MediaType.COLLECTION))
      .map(Parent::title)
      .orElse("")
    );
    model.status.set(work.getStreams().isEmpty() ? MediaStatus.UNAVAILABLE : work.getState().consumed() ? MediaStatus.WATCHED : MediaStatus.AVAILABLE);
  }
}
