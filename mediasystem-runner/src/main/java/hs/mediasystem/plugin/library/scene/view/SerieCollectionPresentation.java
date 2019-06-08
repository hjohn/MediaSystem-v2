package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsSourceFactory;
import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

public class SerieCollectionPresentation extends GridViewPresentation<Serie> {
  private static final MediaType SERIE = MediaType.of("SERIE");

  private static final List<SortOrder<Serie>> SORT_ORDERS = List.of(
    new SortOrder<Serie>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL))),
    new SortOrder<Serie>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())),
    new SortOrder<Serie>("watched-date", Comparator.comparing(mi -> mi.lastWatchedTime.get(), Comparator.<LocalDateTime>nullsFirst(Comparator.naturalOrder()).reversed()))
  );

  private static final List<Filter<Serie>> FILTERS = List.of(
    new Filter<>("none", mi -> true),
    new Filter<>("released-recently", mi -> Optional.ofNullable(mi.getProduction().getDate()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent()),
    new Filter<>("watched-recently", mi -> Optional.ofNullable(mi.lastWatchedTime.get()).filter(d -> d.isAfter(LocalDateTime.now().minusYears(2))).isPresent())
  );

  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private MediaItem.Factory mediaItemFactory;
    @Inject private SettingsSourceFactory settingsSourceFactory;

    public SerieCollectionPresentation create(String tag) {
      return new SerieCollectionPresentation(settingsSourceFactory.of(SYSTEM_PREFIX + "Series:" + tag), mediaService, createProductionItems(mediaService.findAllByType(SERIE, tag, List.of("TMDB", "LOCAL"))));
    }

    private ObservableList<MediaItem<Serie>> createProductionItems(List<Serie> streams) {
      return FXCollections.observableArrayList(streams.stream().map(d -> mediaItemFactory.create(
        d,
        null
      )).collect(Collectors.toList()));
    }
  }

  protected SerieCollectionPresentation(SettingsSource settingsSource, MediaService mediaService, ObservableList<MediaItem<Serie>> items) {
    super(settingsSource, mediaService, items, SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.UNWATCHED));
  }
}
