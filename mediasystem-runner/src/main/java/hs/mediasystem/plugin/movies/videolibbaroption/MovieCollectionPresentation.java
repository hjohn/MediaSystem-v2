package hs.mediasystem.plugin.movies.videolibbaroption;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.scanner.api.MediaType;
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

public class MovieCollectionPresentation extends GridViewPresentation<Movie> {
  private static final MediaType MOVIE = MediaType.of("MOVIE");

  private static final List<SortOrder<Movie>> SORT_ORDERS = List.of(
    new SortOrder<>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL))),
    new SortOrder<>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
  );

  private static final List<Filter<Movie>> FILTERS = List.of(
    new Filter<>("none", mi -> true),
    new Filter<>("released-recently", mi -> Optional.ofNullable(mi.getProduction().getDate()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private MediaItem.Factory mediaItemFactory;
    @Inject private SettingsStore settingsStore;

    public MovieCollectionPresentation create() {
      return new MovieCollectionPresentation(settingsStore, mediaService, createProductionItems(mediaService.findAllByType(MOVIE, List.of("TMDB", "LOCAL"))));
    }

    private ObservableList<MediaItem<Movie>> createProductionItems(List<Movie> mediaSets) {
      return FXCollections.observableArrayList(mediaSets.stream().map(d -> mediaItemFactory.create(
        d,
        null
      )).collect(Collectors.toList()));
    }
  }

  protected MovieCollectionPresentation(SettingsStore settingsStore, MediaService mediaService, ObservableList<MediaItem<Movie>> items) {
    super(settingsStore, mediaService, items, SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.UNWATCHED));
  }
}
