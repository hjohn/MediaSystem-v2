package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Parent;
import hs.mediasystem.ext.basicmediatypes.domain.stream.State;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory.Binder;
import hs.mediasystem.plugin.library.scene.grid.IDBinder;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkBinder implements Binder<Work>, IDBinder<Work> {
  public static final Comparator<Work> BY_NAME = Comparator.comparing(Work::getDetails, Comparator.comparing(Details::getName, NaturalLanguage.ALPHABETICAL));
  public static final Comparator<Work> BY_RELEASE_DATE = Comparator.comparing(Work::getDetails, Comparator.comparing((Details d) -> d.getDate().orElse(null), Comparator.nullsLast(Comparator.naturalOrder())));
  public static final Comparator<Work> BY_LAST_WATCHED_DATE = Comparator.comparing(Work::getState, Comparator.comparing((State d) -> d.getLastWatchedTime().orElse(null), Comparator.nullsLast(Comparator.naturalOrder())));

  private static final MediaType EPISODE = MediaType.of("EPISODE");
  private static final MediaType SERIE = MediaType.of("SERIE");
  private static final MediaType COLLECTION = MediaType.of("COLLECTION");

  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private StreamStateService streamStateService;

  @Override
  public Class<Work> getType() {
    return Work.class;
  }

  @Override
  public Function<Work, ObservableValue<? extends String>> titleBindProvider() {
    return r -> new SimpleStringProperty(r.getDetails().getName());
  }

  @Override
  public Function<Work, ImageHandle> imageHandleExtractor() {
    return r -> r.getDetails().getImage()
      .or(() -> Optional.of(r).flatMap(Work::getPrimaryStream).map(MediaStream::getId).map(StreamID::asInt).map(id -> new ImageURI("localdb://" + id + "/1")))
      .map(imageHandleFactory::fromURI)
      .orElse(null);
  }

  @Override
  public Function<Work, ObservableValue<? extends String>> sideBarTopLeftBindProvider() {
    return r -> {
      if(r.getType().equals(EPISODE)) {
        Episode episode = (Episode)r.getDescriptor();

        return new SimpleStringProperty(episode.getSeasonNumber() == 0 ? "Special " : "Ep. " + episode.getNumber());
      }

      return new SimpleStringProperty(createYearRange(r));
    };
  }

  @Override
  public Function<Work, ObservableValue<? extends String>> sideBarCenterBindProvider() {
    return r -> new SimpleStringProperty(r.getParent()
      .filter(p -> p.getType().equals(COLLECTION))
      .map(Parent::getName)
      .orElse(""));
  }

  @Override
  public Optional<BooleanProperty> watchedProperty(Work work) {
    return work.getPrimaryStream().map(MediaStream::getId).map(streamStateService::watchedProperty);
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
    if(item.getType().equals(SERIE) && item.getDescriptor() instanceof Serie) {  // TODO remove Serie check here
      return createSerieYearRange(item);
    }

    return item.getDetails().getDate().map(LocalDate::getYear).map(Object::toString).orElse("");
  }

  private static String createSerieYearRange(Work item) {
    Serie serie = (Serie)item.getDescriptor();
    LocalDate date = item.getDetails().getDate().orElse(null);

    if(date == null) {
      return "";
    }

    String year = "" + date.getYear();

    if((serie.getState() == Serie.State.CANCELED || serie.getState() == Serie.State.ENDED) && serie.getLastAirDate() != null && serie.getLastAirDate().getYear() != date.getYear()) {
      year += " - " + serie.getLastAirDate().getYear();
    }
    else if(serie.getState() == Serie.State.CONTINUING) {
      year += " -";
    }

    return year;
  }
}
