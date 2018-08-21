package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.scan.StreamPrint;
import hs.mediasystem.framework.actions.Expose;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.Dialogs;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;

import javax.inject.Inject;

public class GridViewPresentation extends AbstractPresentation {

  @Expose(values = "availableSortOrders")
  public final ObjectProperty<SortOrder<?>> sortOrder = objectProperty();
  public final ObservableList<SortOrder<?>> availableSortOrders = FXCollections.observableArrayList();

  @Expose(values = "availableFilters")
  public final ObjectProperty<Filter<?>> filter = objectProperty();
  public final ObservableList<Filter<?>> availableFilters = FXCollections.observableArrayList();

  @Expose
  public final BooleanProperty includeViewed = booleanProperty(true);

  public final ObjectProperty<MediaItem<?>> selectedItem = objectProperty();

  @Inject private StreamStateService streamStateService;
  @Inject private LocalMediaManager localMediaManager;

  @Expose
  public void toggleWatchedState() {
    MediaItem<?> mediaItem = selectedItem.get();

    if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
      StreamPrint streamPrint = mediaItem.getStreams().iterator().next().getStreamPrint();

      // Update state
      boolean watched = streamStateService.isWatched(streamPrint);
      streamStateService.setWatched(streamPrint, !watched);

      // Update MediaItem
      mediaItem.watchedCount.set(!watched ? mediaItem.availableCount.get() : 0);
    }
  }

  private Boolean isWatched() {
    MediaItem<?> mediaItem = selectedItem.get();

    if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
      StreamPrint streamPrint = mediaItem.getStreams().iterator().next().getStreamPrint();

      return streamStateService.isWatched(streamPrint);
    }

    return null;  // Indicates no state possible as there is no stream
  }

  @Expose
  public void reidentify() {
    MediaItem<?> mediaItem = selectedItem.get();

    System.out.println(">>>> REIDENTIFYING " + mediaItem);

    if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
      localMediaManager.reidentify(mediaItem.getStreams().iterator().next());
    }
  }

  enum Option {
    MARK_WATCHED,
    MARK_UNWATCHED,
    REIDENTIFY
  }

  @Expose
  public void showContextMenu(Event event) {
    // Note: map is sorted according to order of Option enum declaration
    Map<Option, String> map = new EnumMap<>(Option.class);

    Boolean isWatched = isWatched();

    if(isWatched != null) {
      if(isWatched) {
        map.put(Option.MARK_UNWATCHED, "Mark Not Watched");
      }
      else {
        map.put(Option.MARK_UNWATCHED, "Mark Watched");
      }
    }

    map.put(Option.REIDENTIFY, "Reidentify");

    Dialogs.show(event, map).ifPresent(option -> {
      switch(option) {
      case MARK_WATCHED:
      case MARK_UNWATCHED:
        toggleWatchedState();
        break;
      case REIDENTIFY:
        reidentify();
        break;
      }
    });
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
