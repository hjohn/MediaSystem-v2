package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsSourceFactory;
import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.db.MediaService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

public class RecommendationsPresentation extends GridViewPresentation<Production> {
  private static final List<SortOrder<MediaDescriptor>> SORT_ORDERS = List.of(
    new SortOrder<>("best", null),
    new SortOrder<>("alpha", MediaItem.BY_NAME),
    new SortOrder<>("release-date", MediaItem.BY_RELEASE_DATE.reversed())
  );

  private static final List<Filter<Production>> FILTERS = List.of(
    new Filter<Production>("none", mi -> true),
    new Filter<Production>("released-recently", mi -> mi.getProduction().getDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private VideoDatabase videoDatabase;
    @Inject private MediaItem.Factory mediaItemFactory;
    @Inject private SettingsSourceFactory settingsSourceFactory;

    public RecommendationsPresentation create(MediaItem<? extends Production> mediaItem) {
      return new RecommendationsPresentation(
        settingsSourceFactory.of(SYSTEM_PREFIX + "Recommendations"),
        mediaService,
        mediaItem,
        FXCollections.observableList(videoDatabase.queryRecommendedProductions(mediaItem.getProduction().getIdentifier()).stream()
          .map(p -> mediaItemFactory.create(p, null))
          .collect(Collectors.toList()))
      );
    }
  }

  @SuppressWarnings("unchecked")
  protected RecommendationsPresentation(SettingsSource settingsSource, MediaService mediaService, MediaItem<? extends Production> mediaItem, ObservableList<MediaItem<Production>> recommendations) {
    super(settingsSource, mediaService, recommendations, new ViewOptions<>(SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.AVAILABLE, StateFilter.UNWATCHED)), (MediaItem<MediaDescriptor>)mediaItem);
  }
}
