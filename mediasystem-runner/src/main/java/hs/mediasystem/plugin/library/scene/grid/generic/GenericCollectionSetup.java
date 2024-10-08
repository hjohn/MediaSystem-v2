package hs.mediasystem.plugin.library.scene.grid.generic;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.cell.MediaGridViewCellFactory.Model;
import hs.mediasystem.plugin.library.scene.base.ContextLayout;
import hs.mediasystem.plugin.library.scene.grid.common.AbstractSetup;
import hs.mediasystem.plugin.library.scene.grid.generic.GenericCollectionPresentationFactory.GenericCollectionPresentation;
import hs.mediasystem.runner.grouping.WorksGroup;
import hs.mediasystem.runner.util.grid.MediaStatus;
import hs.mediasystem.ui.api.domain.Context;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GenericCollectionSetup extends AbstractSetup<Work, Object, GenericCollectionPresentation<Work, Object>> {
  @Inject private ContextLayout contextLayout;

  @Override
  protected Node createPreviewPanel(Object item) {
    return contextLayout.createGeneric(item);
  }

  @Override
  protected void fillModel(Object item, Model model) {
    if(item instanceof Work work) {
      model.title.set(work.getDetails().getTitle());
      model.imageHandle.set(work.getDetails().getAnyCover().orElse(null));
      model.annotation1.set(work.getDetails().getSequence().map(this::createSequenceInfo).orElseGet(() -> work.getDetails().getYearRange()));
      model.annotation2.set(work.getContext()
        .filter(c -> c.type().equals(MediaType.COLLECTION))
        .map(Context::title)
        .orElse("")
      );
      model.status.set(work.getStreams().isEmpty() ? MediaStatus.UNAVAILABLE : work.getState().consumed() ? MediaStatus.WATCHED : MediaStatus.AVAILABLE);
    }
    else if(item instanceof WorksGroup wg) {
      model.title.set(wg.getDetails().getTitle());
      model.imageHandle.set(wg.getDetails().getAnyCover().orElse(null));
      model.annotation1.set(toYearRange(wg));
      model.status.set(wg.allWatched() ? MediaStatus.WATCHED : MediaStatus.AVAILABLE);
    }
    else {
      throw new IllegalStateException("Unsupported item type: " + item);
    }
  }

  private String createSequenceInfo(Sequence sequence) {
    if(sequence.type() == Type.SPECIAL) {
      return "Special " + sequence.number();
    }
    if(sequence.type() == Type.EXTRA) {
      return "Extra " + sequence.number();
    }

    return "Ep. " + sequence.number();
  }

  private static String toYearRange(WorksGroup wg) {
    if(wg.getId().getType().equals(MediaType.COLLECTION)) {
      LocalDate earliestDate = null;
      LocalDate latestDate = null;
      LocalDate now = LocalDate.now();

      for(Work work : wg.getChildren()) {
        LocalDate date = work.getDetails().getReleaseDate().orElse(null);

        if(date != null) {
          if(earliestDate == null || earliestDate.isAfter(date)) {
            earliestDate = date;
          }
          if(date.isBefore(now) && (latestDate == null || latestDate.isBefore(date))) {
            latestDate = date;
          }
        }
      }

      if(earliestDate != null && latestDate != null) {
        return earliestDate.getYear() + (latestDate.getYear() != earliestDate.getYear() ? " - " + latestDate.getYear() : "");
      }
    }

    return null;
  }
}
