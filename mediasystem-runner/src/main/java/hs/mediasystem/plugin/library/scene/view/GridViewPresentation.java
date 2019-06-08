package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.scanner.api.BasicStream;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;

import org.reactfx.value.Var;

public class GridViewPresentation<T extends MediaDescriptor> extends AbstractPresentation {
  protected static final String SYSTEM_PREFIX = "MediaSystem:Library:Presentation:";

  public final Var<SortOrder<T>> sortOrder = Var.newSimpleVar(null);
  public final ObservableList<SortOrder<T>> availableSortOrders = FXCollections.observableArrayList();

  public final Var<Filter<T>> filter = Var.newSimpleVar(null);
  public final ObservableList<Filter<T>> availableFilters = FXCollections.observableArrayList();

  public final Var<StateFilter> stateFilter = Var.newSimpleVar(StateFilter.ALL);
  public final ObservableList<StateFilter> availableStateFilters = FXCollections.observableArrayList();

  public final Var<MediaItem<?>> selectedItem = Var.newSimpleVar(null);
  public final ObservableList<MediaItem<T>> items;

  private final MediaService mediaService;

  public enum StateFilter {
    ALL, AVAILABLE, UNWATCHED
  }

  protected GridViewPresentation(SettingsSource settingsSource, MediaService mediaService, ObservableList<MediaItem<T>> items, List<SortOrder<T>> sortOrders, List<Filter<T>> filters, List<StateFilter> stateFilters) {
    this.mediaService = mediaService;
    this.items = items;

    this.availableSortOrders.setAll(sortOrders);
    this.availableFilters.setAll(filters);
    this.availableStateFilters.setAll(stateFilters);

    this.sortOrder.setValue(sortOrders.get(settingsSource.getIntSettingOrDefault("sort-order", 0, 0, sortOrders.size() - 1)));
    this.filter.setValue(filters.get(settingsSource.getIntSettingOrDefault("filter", 0, 0, filters.size() - 1)));
    this.stateFilter.setValue(stateFilters.get(settingsSource.getIntSettingOrDefault("state-filter", 0, 0, stateFilters.size() - 1)));

    this.sortOrder.addListener(obs -> settingsSource.storeIntSetting("sort-order", sortOrders.indexOf(sortOrder.getValue())));
    this.filter.addListener(obs -> settingsSource.storeIntSetting("filter", availableFilters.indexOf(filter.getValue())));
    this.stateFilter.addListener(obs -> settingsSource.storeIntSetting("state-filter", availableStateFilters.indexOf(stateFilter.getValue())));
  }

  public BooleanProperty watchedProperty() {
    MediaItem<?> mediaItem = selectedItem.getValue();

    if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
      return mediaItem.watched;
    }

    return null;  // Indicates no state possible as there is no stream
  }

  public Task<Void> reidentify(Event event) {
    MediaItem<?> mediaItem = selectedItem.getValue();

    event.consume();

    if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
      return new Task<>() {
        @Override
        protected Void call() throws Exception {
          for(BasicStream basicStream : mediaItem.getStreams()) {
            mediaService.reidentify(basicStream.getId());
          }

          return null;
        }
      };

      // TODO after reidentify reload
      // 1) Replace item in list (or reload entire thing)
      // 2) Position may jump, depending on sorting
      // 3) Remember, task method may be called async...
    }

    return null;
  }

  public static class SortOrder<T extends MediaDescriptor> {
    public final String resourceKey;
    public final Comparator<MediaItem<T>> comparator;

    public SortOrder(String resourceKey, Comparator<MediaItem<T>> comparator) {
      this.resourceKey = resourceKey;
      this.comparator = comparator;
    }
  }

  public static class Filter<T extends MediaDescriptor> {
    public final String resourceKey;
    public final Predicate<MediaItem<T>> predicate;

    public Filter(String resourceKey, Predicate<MediaItem<T>> predicate) {
      this.resourceKey = resourceKey;
      this.predicate = predicate;
    }
  }
}
