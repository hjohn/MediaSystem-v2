package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.cell.MediaGridViewCellFactory.Model;
import hs.mediasystem.plugin.library.scene.MediaStatus;
import hs.mediasystem.plugin.library.scene.base.ContextLayout;
import hs.mediasystem.plugin.library.scene.grid.RecommendationsPresentationFactory.RecommendationsPresentation;
import hs.mediasystem.ui.api.domain.Parent;
import hs.mediasystem.ui.api.domain.Work;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecommendationsSetup extends AbstractSetup<Work, Work, RecommendationsPresentation> {
  @Inject private ContextLayout contextLayout;

  @Override
  protected Node createPreviewPanel(Work item) {
    return contextLayout.create(item);
  }

  @Override
  protected void fillModel(Work work, Model model) {
    model.title.set(work.getDetails().getTitle());
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
