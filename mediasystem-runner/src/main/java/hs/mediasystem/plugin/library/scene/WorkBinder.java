package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory.Binder;
import hs.mediasystem.plugin.library.scene.grid.IDBinder;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Serie;
import hs.mediasystem.ui.api.domain.Stage;
import hs.mediasystem.ui.api.domain.State;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Var;

@Singleton
public class WorkBinder implements Binder<Work>, IDBinder<Work> {
  public static final Comparator<Work> BY_NAME = Comparator.comparing(Work::getDetails, Comparator.comparing(Details::getTitle, NaturalLanguage.ALPHABETICAL));
  public static final Comparator<Work> BY_RELEASE_DATE = Comparator.comparing(Work::getDetails, Comparator.comparing((Details d) -> d.getReleaseDate().orElse(null), Comparator.nullsLast(Comparator.naturalOrder())));
  public static final Comparator<Work> BY_REVERSE_RELEASE_DATE = Comparator.comparing(Work::getDetails, Comparator.comparing((Details d) -> d.getReleaseDate().orElse(null), Comparator.nullsFirst(Comparator.naturalOrder()))).reversed();
  public static final Comparator<Work> BY_LAST_WATCHED_DATE = Comparator.comparing(Work::getState, Comparator.comparing((State d) -> d.getLastConsumptionTime().getValue(), Comparator.nullsLast(Comparator.naturalOrder())));

  private static final MediaType SERIE = MediaType.of("SERIE");
  private static final MediaType COLLECTION = MediaType.of("COLLECTION");

  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  public Class<Work> getType() {
    return Work.class;
  }

  @Override
  public Function<Work, ObservableValue<? extends String>> titleBindProvider() {
    return r -> new SimpleStringProperty(r.getDetails().getTitle());
  }

  @Override
  public Function<Work, ImageHandle> imageHandleExtractor() {
    return r -> r.getDetails().getImage()
      .or(() -> Optional.of(r).flatMap(Work::getPrimaryStream).map(MediaStream::getId).map(StreamID::getContentId).map(ContentID::asInt).map(id -> new ImageURI("localdb://" + id + "/1")))
      .map(imageHandleFactory::fromURI)
      .orElse(null);
  }

  @Override
  public Function<Work, ObservableValue<? extends String>> sideBarTopLeftBindProvider() {
    return r -> new SimpleStringProperty(r.getDetails().getSequence().map(this::createSequenceInfo).orElseGet(() -> createYearRange(r)));
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
    return r -> new SimpleStringProperty(r.getParent()
      .filter(p -> p.getType().equals(COLLECTION))
      .map(Parent::getName)
      .orElse("")
    );
  }

  @Override
  public Var<Boolean> watchedProperty(Work work) {
    return work.getState().isConsumed();
  }

  @Override
  public Optional<Boolean> hasStream(Work work) {
    return Optional.of(!work.getStreams().isEmpty());
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

    Stage stage = details.getClassification().getStage();
    LocalDate lastAirDate = details.getSerie().flatMap(Serie::getLastAirDate).orElse(null);

    if(stage == Stage.ENDED && lastAirDate != null && lastAirDate.getYear() != date.getYear()) {
      return date.getYear() + " - " + lastAirDate.getYear();
    }
    else if(item.getType().equals(SERIE) && stage == Stage.RELEASED) {
      return date.getYear() + " -";
    }

    return "" + date.getYear();
  }
}
