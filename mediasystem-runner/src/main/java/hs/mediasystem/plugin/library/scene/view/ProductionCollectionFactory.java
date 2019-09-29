package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.StateFilter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.ViewOptions;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProductionCollectionFactory {
  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private VideoDatabase videoDatabase;
  @Inject private MediaItem.Factory mediaItemFactory;

  private static final List<SortOrder<MediaDescriptor>> SORT_ORDERS = List.of(
    new SortOrder<>("release-date", MediaItem.BY_RELEASE_DATE.reversed()),
    new SortOrder<>("alpha", MediaItem.BY_NAME)
  );

  private static final List<Filter<Production>> FILTERS = List.of(
    new Filter<Production>("none", mi -> true),
    new Filter<Production>("released-recently", mi -> mi.getProduction().getDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  public GenericCollectionPresentation<Production> create(Identifier collectionIdentifier) {
    ProductionCollection productionCollection = videoDatabase.queryProductionCollection(collectionIdentifier);

    ObservableList<MediaItem<Production>> items = FXCollections.observableList(productionCollection.getItems().stream()
      .map(p -> mediaItemFactory.create(p, null))
      .collect(Collectors.toList()));

    return factory.create(items, "Collections", new ViewOptions<>(SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.AVAILABLE, StateFilter.UNWATCHED)), productionCollection);
  }
}
