package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Release;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.scanner.api.StreamPrintProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    @Inject private StreamPrintProvider streamPrintProvider;

    public <T extends MediaDescriptor> MediaItem<T> create(T descriptor, MediaItem<?> parent) {
      Release release = getRelease(descriptor);
      Set<BasicStream> streams = release == null ? Collections.emptySet() : mediaService.findStreams(release.getIdentifier());
      List<BasicStream> sortedStreams = streams.stream()
        .sorted(Comparator.<BasicStream, Long>comparing(s -> streamPrintProvider.get(s.getId()).getSize(), Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
        .collect(Collectors.toList());

      StreamID streamId = sortedStreams.isEmpty() ? null : sortedStreams.get(0).getId();

      return new MediaItem<>(
        descriptor,
        parent,
        sortedStreams,
        streamId == null ? new SimpleBooleanProperty() : streamStateService.watchedProperty(streamId),
        streamId == null ? null : streamStateService.getLastWatchedTime(streamId)
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
  public final StringProperty productionTitle = new SimpleStringProperty();
  public final StringProperty productionYearRange = new SimpleStringProperty();
  public final StringProperty personName = new SimpleStringProperty();
  public final ObjectBinding<MediaStatus> mediaStatus;
  public final ObjectProperty<LocalDateTime> lastWatchedTime = new SimpleObjectProperty<>();

  private final T wrappedObject;
  private final List<BasicStream> streams;
  private final String logicalId;
  private final String physicalId;
  private final MediaItem<?> parent;

  private MediaItem(T wrappedObject, MediaItem<?> parent, List<BasicStream> streams, BooleanProperty watchedProperty, LocalDateTime lastWatchedTime) {
    if(wrappedObject == null) {
      throw new IllegalArgumentException("wrappedObject cannot be null");
    }

    this.wrappedObject = wrappedObject;
    this.parent = parent;
    this.streams = streams;
    this.watched = watchedProperty;
    this.lastWatchedTime.set(lastWatchedTime);

    this.mediaStatus = new ObjectBinding<>() {
      {
        bind(MediaItem.this.watched);
      }

      @Override
      protected MediaStatus computeValue() {
        return MediaItem.this.streams.isEmpty() ? MediaStatus.UNAVAILABLE :
                   MediaItem.this.watched.get() ? MediaStatus.WATCHED : MediaStatus.AVAILABLE;
      }
    };

    this.missing = mediaStatus.isEqualTo(MediaStatus.UNAVAILABLE);

    Release release = getRelease();

    if(release != null) {
      productionTitle.set(release.getName());
      productionYearRange.set(createYearRange(release));
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

  private static String createYearRange(Release release) {
    if(release instanceof Serie) {
      Serie serie = (Serie)release;

      if(serie.getDate() == null) {
        return "";
      }

      String year = "" + serie.getDate().getYear();

      if(serie.getState() == Serie.State.ENDED && serie.getLastAirDate() != null && serie.getLastAirDate().getYear() != serie.getDate().getYear()) {
        year += " - " + serie.getLastAirDate().getYear();
      }
      else if(serie.getState() == Serie.State.CONTINUING) {
        year += " -";
      }

      return year;
    }

    return Optional.ofNullable(release.getDate()).map(LocalDate::getYear).map(Object::toString).orElse("");
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

  private String createLogicalId() {
    return wrappedObject.getClass().getSimpleName() + ":" + wrappedObject.getIdentifier();
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

  public Production getProduction() {
    return getProduction(wrappedObject);
  }

  public Release getRelease() {
    return getRelease(wrappedObject);
  }

  private static Release getRelease(Object wrappedObject) {
    return wrappedObject instanceof Release ? (Release)wrappedObject :
           wrappedObject instanceof ProductionRole ? ((ProductionRole)wrappedObject).getProduction() : null;
  }

  private static Production getProduction(Object wrappedObject) {
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
    return "MediaItem[" + wrappedObject + "]";
  }
}
