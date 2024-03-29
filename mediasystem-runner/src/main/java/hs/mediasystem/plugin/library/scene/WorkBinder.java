package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.cell.MediaGridViewCellFactory.Binder;
import hs.mediasystem.plugin.library.scene.grid.IDBinder;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Parent;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Serie;
import hs.mediasystem.ui.api.domain.Stage;
import hs.mediasystem.ui.api.domain.State;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Singleton;

@Singleton
public class WorkBinder implements Binder<Work>, IDBinder<Work> {
  public static final Comparator<Work> BY_NAME = Comparator.comparing(Work::getDetails, Details.ALPHABETICAL);
  public static final Comparator<Work> BY_SUBTITLE = Comparator.comparing(Work::getDetails, Comparator.comparing(d -> d.getSubtitle().orElse(null), Comparator.nullsLast(NaturalLanguage.ALPHABETICAL)));
  public static final Comparator<Work> BY_RELEASE_DATE = Comparator.comparing(Work::getDetails, Details.RELEASE_DATE);
  public static final Comparator<Work> BY_REVERSE_RELEASE_DATE = Comparator.comparing(Work::getDetails, Details.RELEASE_DATE_REVERSED);
  public static final Comparator<Work> BY_LAST_WATCHED_DATE = Comparator.comparing(Work::getState, Comparator.comparing((State d) -> d.getLastConsumptionTime().orElse(null), Comparator.nullsLast(Comparator.naturalOrder())));

  @Override
  public Class<Work> getType() {
    return Work.class;
  }

  @Override
  public Function<Work, ObservableValue<? extends String>> titleBindProvider() {
    return w -> new SimpleStringProperty(w.getDetails().getTitle());
  }

  @Override
  public Function<Work, ImageHandle> imageHandleExtractor() {
    return w -> w.getDetails().getCover().or(w.getDetails()::getSampleImage).orElse(null);
  }

  @Override
  public Function<Work, ObservableValue<? extends String>> sideBarTopLeftBindProvider() {
    return w -> new SimpleStringProperty(w.getDetails().getSequence().map(this::createSequenceInfo).orElseGet(() -> createYearRange(w)));
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

  @Override
  public Function<Work, ObservableValue<? extends String>> sideBarCenterBindProvider() {
    return w -> new SimpleStringProperty(w.getParent()
      .filter(p -> p.getType().equals(MediaType.COLLECTION))
      .map(Parent::getName)
      .orElse("")
    );
  }

  @Override
  public boolean watchedProperty(Work work) {
    return work.getState().isConsumed();
  }

  @Override
  public boolean hasStream(Work work) {
    return !work.getStreams().isEmpty();
  }

  @Override
  public Function<Work, String> detailExtractor() {
    return null;
  }

  @Override
  public String toId(Work item) {
    return item.getId().toString();
  }

  public static String createYearRange(Work item) {
    Details details = item.getDetails();
    LocalDate date = details.getReleaseDate().orElse(null);

    if(date == null) {
      return "";
    }

    Stage stage = details.getClassification().getStage().orElse(null);
    LocalDate lastAirDate = details.getSerie().flatMap(Serie::getLastAirDate).orElse(null);

    if(stage == Stage.ENDED && lastAirDate != null && lastAirDate.getYear() != date.getYear()) {
      return date.getYear() + " - " + lastAirDate.getYear();
    }
    else if(item.getType().isSerie() && stage == Stage.RELEASED) {
      return date.getYear() + " -";
    }

    return "" + date.getYear();
  }
}
