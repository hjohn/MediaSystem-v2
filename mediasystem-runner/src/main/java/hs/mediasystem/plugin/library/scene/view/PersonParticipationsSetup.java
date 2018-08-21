package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
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
public class PersonParticipationsSetup extends AbstractSetup<ProductionRole, PersonParticipationsPresentation> {
  @Inject private VideoDatabase database;
  @Inject private LocalMediaManager localMediaManager;
  @Inject private ContextLayout contextLayout;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private StreamStateService streamStateService;
  @Inject private PresentationLoader presentationLoader;
  @Inject private ProductionPresentation.Factory productionPresentationFactory;

  @Override
  public ObservableList<MediaItem<ProductionRole>> getItems(PersonParticipationsPresentation presentation) {
    return FXCollections.observableList(presentation.personalProfile.get().getProductionRoles().stream().map(this::wrap).collect(Collectors.toList()));
  }

  private int countWatchedStreams(Collection<MediaStream<?>> streams) {
    for(MediaStream<?> stream : streams) {
      if(streamStateService.isWatched(stream.getStreamPrint())) {
        return 1;
      }
    }

    return 0;
  }

  private MediaItem<ProductionRole> wrap(ProductionRole data) {
    Set<MediaStream<?>> streams = localMediaManager.find(data.getProduction().getIdentifier());

    return new MediaItem<>(data, null, streams, countWatchedStreams(streams), streams.isEmpty() ? 0 : 1);
  }

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<ProductionRole> cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
    cellFactory.setDetailExtractor(
      m -> formatDetail(m)
    );
  }

  private static String formatDetail(MediaItem<ProductionRole> m) {
    Role role = m.getRole();
    ProductionRole productionRole = m.getData();

    String detail = role.getCharacter() != null && !role.getCharacter().isEmpty() ? "as " + role.getCharacter()
      : role.getJob() != null && !role.getJob().isEmpty() ? role.getJob() : "";

    if(productionRole.getEpisodeCount() != null && productionRole.getEpisodeCount() > 0) {
      detail += " (" + productionRole.getEpisodeCount() + ")";
    }

    return detail;
  }

  @Override
  protected Node createContextPanel(PersonParticipationsPresentation presentation) {
    return contextLayout.create(presentation.personalProfile.get());
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<ProductionRole>> event, PersonParticipationsPresentation presentation) {
    Production production = database.queryProduction(event.getItem().getProduction().getIdentifier());

    presentationLoader.loadAndNavigate(event, () -> productionPresentationFactory.create(new MediaItem<>(production, null, event.getItem().getStreams(), event.getItem().watchedCount.get(), event.getItem().availableCount.get())));
  }

  @Override
  protected List<SortOrder<ProductionRole>> getAvailableSortOrders() {
    return List.of(
      new SortOrder<>("popularity", Comparator.comparing((MediaItem<ProductionRole> mediaItem) -> mediaItem.getData().getPopularity()).reversed()),
      new SortOrder<ProductionRole>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL))),
      new SortOrder<ProductionRole>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
    );
  }

  @Override
  protected List<Filter<ProductionRole>> getAvailableFilters() {
    return List.of(
      new Filter<ProductionRole>("none", mi -> true),
      new Filter<ProductionRole>("cast", mi -> mi.getRole().getType() != Role.Type.CREW),
      new Filter<ProductionRole>("crew", mi -> mi.getRole().getType() == Role.Type.CREW)
    );
  }

  @Override
  protected boolean showViewed() {
    return true;
  }

  @Override
  public Node create(PersonParticipationsPresentation presentation) {
    return createView(presentation);
  }
}
