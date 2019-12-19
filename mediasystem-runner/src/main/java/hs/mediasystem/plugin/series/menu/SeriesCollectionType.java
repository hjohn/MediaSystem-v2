package hs.mediasystem.plugin.series.menu;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.StateFilter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.ViewOptions;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.collection.CollectionType;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.scanner.api.MediaType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SeriesCollectionType implements CollectionType {
  private static final MediaType SERIE = MediaType.of("SERIE");

  private static final List<SortOrder<MediaDescriptor>> SORT_ORDERS = List.of(
    new SortOrder<>("alpha", MediaItem.BY_NAME),
    new SortOrder<>("release-date", MediaItem.BY_RELEASE_DATE.reversed()),
    new SortOrder<>("watched-date", MediaItem.BY_WATCHED_DATE.reversed())
  );

  private static final List<Filter<Serie>> FILTERS = List.of(
    new Filter<>("none", mi -> true),
    new Filter<>("released-recently", mi -> mi.getProduction().getDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent()),
    new Filter<>("watched-recently", mi -> Optional.ofNullable(mi.lastWatchedTime.get()).filter(d -> d.isAfter(Instant.now().minus(2, ChronoUnit.YEARS))).isPresent())
  );

  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private MediaService mediaService;
  @Inject private MediaItem.Factory mediaItemFactory;

  @Override
  public String getId() {
    return SERIE.toString();
  }

  @Override
  public Presentation createPresentation(String tag) {
    return factory.create(
      createProductionItems(mediaService.findAllByType(SERIE, tag, List.of("TMDB", "LOCAL"))),
      "Series" + (tag == null ? "" : ":" + tag),
      new ViewOptions<>(SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.UNWATCHED)),
      null
    );
  }

  private ObservableList<MediaItem<Serie>> createProductionItems(List<Serie> descriptors) {
    return FXCollections.observableArrayList(descriptors.stream().map(d -> mediaItemFactory.create(d, null)).collect(Collectors.toList()));
  }
}
