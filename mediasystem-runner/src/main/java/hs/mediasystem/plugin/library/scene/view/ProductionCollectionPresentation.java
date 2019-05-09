package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.mediamanager.MediaService;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

// TODO use Generic one
public class ProductionCollectionPresentation extends GridViewPresentation<Production> {
  public final ProductionCollection productionCollection;

  private static final List<SortOrder<Production>> SORT_ORDERS = List.of(
    new SortOrder<Production>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())))),
    new SortOrder<Production>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL)))
  );

  private static final List<Filter<Production>> FILTERS = List.of(
    new Filter<Production>("none", mi -> true),
    new Filter<Production>("released-recently", mi -> Optional.ofNullable(mi.getProduction().getDate()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private VideoDatabase videoDatabase;
    @Inject private SettingsStore settingsStore;

    public ProductionCollectionPresentation create(Identifier collectionIdentifier) {
      return new ProductionCollectionPresentation(
        settingsStore,
        mediaService,
        videoDatabase.queryProductionCollection(collectionIdentifier)
      );
    }
  }

  protected ProductionCollectionPresentation(SettingsStore settingsStore, MediaService mediaService, ProductionCollection productionCollection) {
    super(settingsStore, mediaService, SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.AVAILABLE, StateFilter.UNWATCHED));

    this.productionCollection = productionCollection;
  }
}
