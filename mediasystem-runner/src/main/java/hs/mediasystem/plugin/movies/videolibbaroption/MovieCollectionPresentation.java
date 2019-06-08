package hs.mediasystem.plugin.movies.videolibbaroption;

import hs.mediasystem.db.SettingsSourceFactory;
import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.domain.CollectionDetails;
import hs.mediasystem.ext.basicmediatypes.domain.DetailedMediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

public class MovieCollectionPresentation extends GridViewPresentation<DetailedMediaDescriptor> {
  private static final MediaType MOVIE = MediaType.of("MOVIE");

  private static final List<SortOrder<DetailedMediaDescriptor>> SORT_ORDERS = List.of(
    new SortOrder<>("alpha", Comparator.comparing(MediaItem::getDetails, Comparator.comparing(Details::getName, NaturalLanguage.ALPHABETICAL))),
    new SortOrder<>("release-date", Comparator.comparing((MediaItem<DetailedMediaDescriptor> mi) -> mi.date.get(), Comparator.nullsLast(Comparator.naturalOrder())).reversed())
  );

  private static final List<Filter<DetailedMediaDescriptor>> FILTERS = List.of(
    new Filter<>("none", mi -> true),
    new Filter<>("released-recently", mi -> Optional.ofNullable(mi.date.get()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<Filter<DetailedMediaDescriptor>> GROUPS = List.of(
    new Filter<>("grouped", mi -> mi.getData() instanceof Movie ? ((Movie)mi.getData()).getCollectionDetails() == null : true),
    new Filter<>("ungrouped", mi -> !(mi.getData() instanceof ProductionCollection))
  );

  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private MediaItem.Factory mediaItemFactory;
    @Inject private SettingsSourceFactory settingsSourceFactory;
    @Inject private VideoDatabase videoDatabase;

    public MovieCollectionPresentation create() {
      ObservableList<MediaItem<Movie>> productionItems = createProductionItems(mediaService.findAllByType(MOVIE, List.of("TMDB", "LOCAL")));

      List<MediaItem<DetailedMediaDescriptor>> groups = productionItems.stream()
        .map(mi -> mi.getData().getCollectionDetails())
        .filter(Objects::nonNull)
        .map(CollectionDetails::getIdentifier)
        .distinct()
        .map(videoDatabase::queryProductionCollection)
        .map(DetailedMediaDescriptor.class::cast)
        .map(pc -> mediaItemFactory.create(pc, null))
        .collect(Collectors.toList());

      @SuppressWarnings("unchecked")
      ObservableList<MediaItem<DetailedMediaDescriptor>> items = (ObservableList<MediaItem<DetailedMediaDescriptor>>)(ObservableList<?>)productionItems;

      items.addAll(groups);

      return new MovieCollectionPresentation(settingsSourceFactory.of(SYSTEM_PREFIX + "Movies"), mediaService, items);
    }

    private ObservableList<MediaItem<Movie>> createProductionItems(List<Movie> mediaSets) {
      return FXCollections.observableArrayList(mediaSets.stream().map(d -> mediaItemFactory.create(
        d,
        null
      )).collect(Collectors.toList()));
    }
  }

  protected MovieCollectionPresentation(SettingsSource settingsSource, MediaService mediaService, ObservableList<MediaItem<DetailedMediaDescriptor>> items) {
    super(settingsSource, mediaService, items, SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.UNWATCHED), GROUPS);
  }
}
