package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.MovieDescriptor;
import hs.mediasystem.ext.basicmediatypes.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory.MediaStatus;

import java.util.Set;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MediaItem<T> {
  public final IntegerProperty watchedCount = new SimpleIntegerProperty();
  public final IntegerProperty availableCount = new SimpleIntegerProperty();

  public final StringProperty productionTitle = new SimpleStringProperty();
  public final StringProperty personName = new SimpleStringProperty();
  public final ObjectBinding<MediaStatus> mediaStatus; // = new SimpleObjectProperty<>();//TODO move MediaStatus enum to this class

  // Movie: UNAVAILABLE -> 0 0
  // Movie: AVAILABLE   -> 0 1
  // Movie: WATCHED     -> 1 1
  // Serie: 10 episodes, 5 watched, 7 available -> 5/7 or 5/10 ?

  private final T wrappedObject;
  private final Set<MediaStream<?>> streams;
  private final String id;

  public MediaItem(T wrappedObject, Set<MediaStream<?>> streams, int watchedCount, int availableCount) {
    if(wrappedObject == null) {
      throw new IllegalArgumentException("wrappedObject cannot be null");
    }

    this.wrappedObject = wrappedObject;
    this.streams = streams;

    // Set properties
    this.watchedCount.set(watchedCount);
    this.availableCount.set(availableCount);

    this.mediaStatus = new ObjectBinding<>() {
      {
        bind(MediaItem.this.watchedCount, MediaItem.this.availableCount);
      }

      @Override
      protected MediaStatus computeValue() {
        return MediaItem.this.availableCount.get() == 0 ? MediaStatus.UNAVAILABLE :
          MediaItem.this.availableCount.get() == MediaItem.this.watchedCount.get() ? MediaStatus.WATCHED : MediaStatus.AVAILABLE;
      }
    };

    Production production = getProduction();

    if(production != null) {
      productionTitle.set(production.getName());
    }

    Person person = getPerson();

    if(person != null) {
      personName.set(person.getName());
    }

    this.id = createId();
  }

  public String getId() {
    return id;
  }

  private String createId() {
    if(streams.isEmpty()) {
      String id = wrappedObject.getClass().getSimpleName() + ":";
      Production production = getProduction();
      Role role = getRole();
      Person person = getPerson();

      if(production != null) {
        id += "Production:" + production.getIdentifier();
      }
      if(role != null) {
        id += "Role:" + role.getIdentifier();
      }
      if(person != null) {
        id += "Person:" + person.getIdentifier();
      }

      return id;
    }

    return "StreamPrint:" + streams.iterator().next().getStreamPrint().getIdentifier();
  }

  public Set<MediaStream<?>> getStreams() {
    return streams;
  }

  public T getData() {
    return wrappedObject;
  }

  public Production getProduction() {
    return wrappedObject instanceof MovieDescriptor ? ((MovieDescriptor)wrappedObject).getProduction() :
           wrappedObject instanceof Serie ? ((Serie)wrappedObject).getProduction() :
           wrappedObject instanceof Season ? ((Season)wrappedObject).getProduction() :
           wrappedObject instanceof Episode ? ((Episode)wrappedObject).getProduction() :
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
