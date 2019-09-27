package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Release;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.scanner.api.StreamPrintProvider;
import hs.mediasystem.util.Exceptional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

public class MediaItem<T extends MediaDescriptor> {
  @Singleton
  public static class Factory {
    @Inject private StreamStateService streamStateService;
    @Inject private MediaService mediaService;
    @Inject private VideoDatabase videoDatabase;
    @Inject private StreamPrintProvider streamPrintProvider;

    public <T extends MediaDescriptor> MediaItem<T> createParent(T descriptor, List<MediaItem<? extends MediaDescriptor>> children) {
      return create(descriptor, null, List.copyOf(children));
    }

    public <T extends MediaDescriptor> MediaItem<T> create(T descriptor, MediaItem<?> parent) {
      return create(descriptor, parent, Collections.emptyList());
    }

    private <T extends MediaDescriptor> MediaItem<T> create(T descriptor, MediaItem<?> parent, List<MediaItem<? extends MediaDescriptor>> children) {
      Release release = getRelease(descriptor);
      Set<BasicStream> streams = release == null ? Collections.emptySet() : mediaService.findStreams(release.getIdentifier());
      List<BasicStream> sortedStreams = streams.stream()
        .sorted(Comparator.<BasicStream, Long>comparing(s -> streamPrintProvider.get(s.getId()).getSize(), Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
        .collect(Collectors.toList());

      StreamID streamId = sortedStreams.isEmpty() ? null : sortedStreams.get(0).getId();
      String collectionTitle = null;

      if(descriptor instanceof Production) {
        collectionTitle = Exceptional.of(((Production)descriptor).getCollectionIdentifier())
          .map(videoDatabase::queryProductionCollection)
          .map(ProductionCollection::getDetails)
          .map(Details::getName)
          .ignore(Throwable.class)
          .orElse(null);
      }

      return new MediaItem<>(
        descriptor,
        parent,
        sortedStreams,
        streamId == null ? new SimpleBooleanProperty() : streamStateService.watchedProperty(streamId),
        streamId == null ? null : streamStateService.getLastWatchedTime(streamId),
        collectionTitle,
        children
      );
    }
  }

  public enum MediaStatus {
    UNAVAILABLE,
    AVAILABLE,
    WATCHED
  }

  public final BooleanProperty watched;
  public final BooleanExpression missing;
  public final StringProperty collectionTitle = new SimpleStringProperty();
  public final StringProperty productionTitle = new SimpleStringProperty();
  public final StringProperty productionYearRange = new SimpleStringProperty();
  public final StringProperty personName = new SimpleStringProperty();
  public final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
  public final ObjectProperty<List<String>> genres = new SimpleObjectProperty<>();
  public final ObjectBinding<MediaStatus> mediaStatus;
  public final ObjectProperty<LocalDateTime> lastWatchedTime = new SimpleObjectProperty<>();

  private final T wrappedObject;
  private final List<BasicStream> streams;
  private final String logicalId;
  private final String physicalId;
  private final MediaItem<?> parent;  // This always is the logical parent (ie, Serie for an Episode)
  private final List<MediaItem<? extends MediaDescriptor>> children;  // Children belonging to this item for grouping (not logical children!)

  private MediaItem(T wrappedObject, MediaItem<?> parent, List<BasicStream> streams, BooleanProperty watchedProperty, LocalDateTime lastWatchedTime, String collectionTitle, List<MediaItem<? extends MediaDescriptor>> children) {
    if(wrappedObject == null) {
      throw new IllegalArgumentException("wrappedObject cannot be null");
    }

    this.wrappedObject = wrappedObject;
    this.parent = parent;
    this.streams = streams;
    this.watched = watchedProperty;
    this.lastWatchedTime.set(lastWatchedTime);
    this.collectionTitle.set(collectionTitle);
    this.children = List.copyOf(children);

    this.mediaStatus = new ObjectBinding<>() {
      {
        bind(MediaItem.this.watched);
      }

      @Override
      protected MediaStatus computeValue() {
        if(MediaItem.this.getData() instanceof ProductionCollection || !children.isEmpty()) {
          return MediaStatus.AVAILABLE;
        }

        return MediaItem.this.streams.isEmpty() ? MediaStatus.UNAVAILABLE :
                   MediaItem.this.watched.get() ? MediaStatus.WATCHED : MediaStatus.AVAILABLE;
      }
    };

    this.missing = mediaStatus.isEqualTo(MediaStatus.UNAVAILABLE);

    Details details = getDetails();

    if(details != null) {
      productionTitle.set(details.getName());

      if(getData() instanceof ProductionCollection) {
        ProductionCollection productionCollection = (ProductionCollection)getData();
        LocalDate first = productionCollection.getFirstReleaseDate();
        LocalDate last = productionCollection.getLastReleaseDate();
        LocalDate next = productionCollection.getNextReleaseDate();

        if(first != null) {
          LocalDate max = last != null ? last : next != null ? next : first;
          int minYear = first.getYear();
          int maxYear = max.getYear();

          date.setValue(max);
          productionYearRange.setValue(minYear == maxYear ? "" + minYear : minYear + " - " + maxYear);
        }

        Map<String, Long> genreCounts = productionCollection.getItems().stream()
          .map(Production::getGenres)
          .flatMap(Collection::stream)
          .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        genres.setValue(genreCounts.entrySet().stream()
          .sorted(Comparator.comparing(Map.Entry::getValue, Collections.reverseOrder()))
          .map(Map.Entry::getKey)
          .collect(Collectors.toList())
        );
      }
      else {
        date.set(details.getDate());

        Production production = getProduction();

        if(production != null) {
          genres.setValue(production.getGenres());
        }
        else {
          genres.setValue(Collections.emptyList());
        }

        if(getData() instanceof Serie) {
          productionYearRange.set(createSerieYearRange((Serie)getData()));
        }
        else {
          productionYearRange.set(Optional.ofNullable(date.get()).map(LocalDate::getYear).map(Object::toString).orElse(""));
        }
      }
    }

    Person person = getPerson();

    if(person != null) {
      personName.set(person.getName());
    }

    this.physicalId = createPhysicalId();
    this.logicalId = createLogicalId();

    if(physicalId == null && logicalId == null) {
      throw new IllegalStateException("MediaItem must have an id: " + this);
    }
  }

  private static String createSerieYearRange(Serie serie) {
    if(serie.getDate() == null) {
      return "";
    }

    String year = "" + serie.getDate().getYear();

    if((serie.getState() == Serie.State.CANCELED || serie.getState() == Serie.State.ENDED) && serie.getLastAirDate() != null && serie.getLastAirDate().getYear() != serie.getDate().getYear()) {
      year += " - " + serie.getLastAirDate().getYear();
    }
    else if(serie.getState() == Serie.State.CONTINUING) {
      year += " -";
    }

    return year;
  }

  /**
   * Returns the id for this item.
   *
   * @return the id, never null
   */
  public String getId() {
    return physicalId != null ? physicalId : logicalId;
  }

  /**
   * Returns the id of the phyiscal stream associated with this item.
   *
   * @return the id of the phyiscal stream, can be null if there is no stream
   */
  public String getPhysicalId() {
    return physicalId;
  }

  /**
   * Returns the id of the logical item this item was identified as.
   *
   * @return the id of the logical item this item was identified as, can be null if not identified
   */
  public String getLogicalId() {
    return logicalId;
  }

  public MediaItem<?> getParent() {
    return parent;
  }

  public List<MediaItem<? extends MediaDescriptor>> getChildren() {
    return children;
  }

  private String createLogicalId() {
    return wrappedObject.getIdentifier().toString();
  }

  private String createPhysicalId() {
    if(streams.isEmpty()) {
      return null;
    }

    return "StreamPrint:" + streams.iterator().next().getId().asInt();
  }

  public BasicStream getStream() {
    return getStreams().isEmpty() ? null : getStreams().get(0);
  }

  public List<BasicStream> getStreams() {
    return streams;
  }

  public T getData() {
    return wrappedObject;
  }

  public Details getDetails() {
    return wrappedObject.getDetails();
  }

  public Production getProduction() {
    return getProduction(wrappedObject);
  }

  public Release getRelease() {
    return getRelease(wrappedObject);
  }

  private static Release getRelease(MediaDescriptor wrappedObject) {
    return wrappedObject instanceof Release ? (Release)wrappedObject :
           wrappedObject instanceof ProductionRole ? ((ProductionRole)wrappedObject).getProduction() : null;
  }

  private static Production getProduction(MediaDescriptor wrappedObject) {
    return wrappedObject instanceof Production ? (Production)wrappedObject :
           wrappedObject instanceof ProductionRole ? ((ProductionRole)wrappedObject).getProduction() : null;
  }

  public Role getRole() {
    return wrappedObject instanceof ProductionRole ? ((ProductionRole)wrappedObject).getRole() :
           wrappedObject instanceof PersonRole ? ((PersonRole)wrappedObject).getRole() : null;
  }

  public Person getPerson() {
    return wrappedObject instanceof PersonRole ? ((PersonRole)wrappedObject).getPerson() : null;
  }

  @Override
  public String toString() {
    return "MediaItem[" + getId() + ": " + wrappedObject + "]";
  }
}
