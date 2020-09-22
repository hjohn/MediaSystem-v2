package hs.mediasystem.runner.grouping;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.cell.MediaGridViewCellFactory.Binder;
import hs.mediasystem.plugin.library.scene.grid.IDBinder;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorksGroupBinder implements Binder<WorksGroup>, IDBinder<WorksGroup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  public Class<WorksGroup> getType() {
    return WorksGroup.class;
  }

  @Override
  public Function<WorksGroup, ObservableValue<? extends String>> titleBindProvider() {
    return wg -> new SimpleStringProperty(wg.getDetails().getTitle());
  }

  @Override
  public Function<WorksGroup, ImageHandle> imageHandleExtractor() {
    return wg -> wg.getDetails().getCover().map(imageHandleFactory::fromURI).orElse(null);
  }

  @Override
  public Function<WorksGroup, ObservableValue<? extends String>> sideBarTopLeftBindProvider() {
    return wg -> {
      String yearRange = toYearRange(wg);

      return yearRange == null ? null : new SimpleStringProperty(yearRange);
    };
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

  @Override
  public Optional<Boolean> hasStream(WorksGroup item) {
    return Optional.of(true);
  }

  @Override
  public String toId(WorksGroup item) {
    return item.getId().toString();
  }
}
