package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.StreamStateProvider;
import hs.mediasystem.ext.basicmediatypes.StreamPrint;
import hs.mediasystem.framework.actions.Expose;
import hs.mediasystem.plugin.library.scene.EntityPresentation;
import hs.mediasystem.plugin.library.scene.LibraryLocation;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.LocationPresentation;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class GridViewPresentation extends LocationPresentation<LibraryLocation> {

  @Expose(values = "availableSortOrders")
  public final ObjectProperty<SortOrder<?>> sortOrder = new SimpleObjectProperty<>();
  public final ObservableList<SortOrder<?>> availableSortOrders = FXCollections.observableArrayList();

  @Expose(values = "availableFilters")
  public final ObjectProperty<Filter<?>> filter = new SimpleObjectProperty<>();
  public final ObservableList<Filter<?>> availableFilters = FXCollections.observableArrayList();

  @Expose
  public final BooleanProperty includeViewed = new SimpleBooleanProperty(true);

  public final ObjectProperty<MediaItem<?>> selectedItem = new SimpleObjectProperty<>();

  private final StreamStateProvider streamStateProvider;
  private final EntityPresentation entityPresentation;

  public GridViewPresentation(StreamStateProvider streamStateProvider, EntityPresentation entityPresentation) {
    this.streamStateProvider = streamStateProvider;
    this.entityPresentation = entityPresentation;
  }

  public EntityPresentation getEntityPresentation() {
    return entityPresentation;
  }

  @Expose
  public void toggleWatchedState() {
    MediaItem<?> mediaItem = selectedItem.get();

    if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
      StreamPrint streamPrint = mediaItem.getStreams().iterator().next().getStreamPrint();

      // Update state
      Map<String, Object> map = streamStateProvider.get(streamPrint);

      boolean watched = (boolean)map.getOrDefault("watched", false);

      map.put("watched", !watched);

      // Update MediaItem
      mediaItem.watchedCount.set(!watched ? mediaItem.availableCount.get() : 0);
    }
  }

  public static class SortOrder<T> {
    public final String resourceKey;
    public final Comparator<MediaItem<T>> comparator;

    public SortOrder(String resourceKey, Comparator<MediaItem<T>> comparator) {
      this.resourceKey = resourceKey;
      this.comparator = comparator;
    }
  }

  public static class Filter<T> {
    public final String resourceKey;
    public final Predicate<MediaItem<T>> predicate;

    public Filter(String resourceKey, Predicate<MediaItem<T>> predicate) {
      this.resourceKey = resourceKey;
      this.predicate = predicate;
    }
  }
}
