package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsSourceFactory;
import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

public class RecommendationsPresentation extends GridViewPresentation<Production> {
  public final MediaItem<?> mediaItem;

  private static final List<SortOrder<Production>> SORT_ORDERS = List.of(
    new SortOrder<Production>("best", null),
    new SortOrder<Production>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL))),
    new SortOrder<Production>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
  );

  private static final List<Filter<Production>> FILTERS = List.of(
    new Filter<Production>("none", mi -> true),
    new Filter<Production>("released-recently", mi -> Optional.ofNullable(mi.getProduction().getDate()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private VideoDatabase videoDatabase;
    @Inject private MediaItem.Factory mediaItemFactory;
    @Inject private SettingsSourceFactory settingsSourceFactory;

    public RecommendationsPresentation create(MediaItem<?> mediaItem) {
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

  protected RecommendationsPresentation(SettingsSource settingsSource, MediaService mediaService, MediaItem<?> mediaItem, ObservableList<MediaItem<Production>> recommendations) {
    super(settingsSource, mediaService, recommendations, SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.AVAILABLE, StateFilter.UNWATCHED));

    this.mediaItem = mediaItem;
  }
}
