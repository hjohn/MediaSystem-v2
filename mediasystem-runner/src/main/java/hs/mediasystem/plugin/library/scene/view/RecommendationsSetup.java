package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.ContextLayout;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.util.NaturalLanguage;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecommendationsSetup extends AbstractSetup<Production, RecommendationsPresentation> {
  @Inject private VideoDatabase database;
  @Inject private LocalMediaManager localMediaManager;
  @Inject private ContextLayout contextLayout;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private StreamStateService streamStateService;
  @Inject private PresentationLoader presentationLoader;
  @Inject private ProductionPresentation.Factory productionPresentationFactory;

  @Override
  public ObservableList<MediaItem<Production>> getItems(RecommendationsPresentation presentation) {
    return FXCollections.observableList(database.queryRecommendedProductions(presentation.mediaItem.get().getProduction().getIdentifier()).stream().map(this::wrap).collect(Collectors.toList()));
  }

  private int countWatchedStreams(Collection<MediaStream<?>> streams) {
    for(MediaStream<?> stream : streams) {
      if(streamStateService.isWatched(stream.getStreamPrint())) {
        return 1;
      }
    }

    return 0;
  }

  private MediaItem<Production> wrap(Production data) {
    Set<MediaStream<?>> streams = localMediaManager.find(data.getIdentifier());

    return new MediaItem<>(data, null, streams, countWatchedStreams(streams), streams.isEmpty() ? 0 : 1);
  }

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<Production> cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
  }

  @Override
  protected Node createContextPanel(RecommendationsPresentation presentation) {
    return contextLayout.create(presentation.mediaItem.get());
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<Production>> event, RecommendationsPresentation presentation) {
    Production production = database.queryProduction(event.getItem().getProduction().getIdentifier());

    presentationLoader.loadAndNavigate(event, () -> productionPresentationFactory.create(new MediaItem<>(production, null, event.getItem().getStreams(), event.getItem().watchedCount.get(), event.getItem().availableCount.get())));
  }

  @Override
  protected List<SortOrder<Production>> getAvailableSortOrders() {
    return List.of(
      new SortOrder<Production>("best", null),
      new SortOrder<Production>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL))),
      new SortOrder<Production>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
    );
  }

  @Override
  protected List<Filter<Production>> getAvailableFilters() {
    return List.of(
      new Filter<Production>("none", mi -> true),
      new Filter<Production>("released-recently", mi -> Optional.ofNullable(mi.getProduction().getDate()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
    );
  }

  @Override
  protected boolean showViewed() {
    return true;
  }

  @Override
  public Node create(RecommendationsPresentation presentation) {
    return createView(presentation);
  }
}
