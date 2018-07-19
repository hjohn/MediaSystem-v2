package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.StreamStateProvider;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.ContextLayout;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.runner.SceneNavigator;
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
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class PersonParticipationsSetup extends AbstractSetup<ProductionRole, PersonParticipationsPresentation> {
  @Inject private VideoDatabase database;
  @Inject private LocalMediaManager localMediaManager;
  @Inject private ContextLayout contextLayout;
  @Inject private SceneNavigator navigator;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private StreamStateProvider streamStateProvider;
  @Inject private Provider<ProductionDetailPresentation> detailPresentationProvider;
  @Inject private Provider<ProductionPresentation> productionPresentationProvider;

  @Override
  public ObservableList<MediaItem<?>> getItems(PersonParticipationsPresentation presentation) {
    Person person = presentation.person.get();

    return FXCollections.observableList(database.queryParticipations(person.getIdentifier()).stream().map(this::wrap).collect(Collectors.toList()));
  }

  private int countWatchedStreams(Collection<MediaStream<?>> streams) {
    for(MediaStream<?> stream : streams) {
      if((boolean)streamStateProvider.get(stream.getStreamPrint()).getOrDefault("watched", false)) {
        return 1;
      }
    }

    return 0;
  }

  private MediaItem<ProductionRole> wrap(ProductionRole data) {
    Set<MediaStream<?>> streams = localMediaManager.find(data.getProduction().getIdentifier());

    return new MediaItem<>(data, streams, countWatchedStreams(streams), streams.isEmpty() ? 0 : 1);
  }

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
    cellFactory.setDetailExtractor(
      m -> m.getRole().getCharacter() != null && !m.getRole().getCharacter().isEmpty() ? "as " + m.getRole().getCharacter()
        : m.getRole().getJob() != null && !m.getRole().getJob().isEmpty() ? "(" + m.getRole().getJob() + ")" : ""
    );
  }

  @Override
  protected Node createContextPanel(PersonParticipationsPresentation presentation) {
    Person person = presentation.person.get();

    return contextLayout.create(person);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<?>> event, PersonParticipationsPresentation presentation) {
    navigator.navigateTo(productionPresentationProvider.get().set(event.getItem()));
    event.consume();
  }

  @Override
  protected List<SortOrder<ProductionRole>> getAvailableSortOrders() {
    return List.of(
      new SortOrder<>("popularity", Comparator.comparing((MediaItem<ProductionRole> mediaItem) -> mediaItem.getData().getPopularity()).reversed()),
      new SortOrder<ProductionRole>("alpha", Comparator.comparing(mediaItem -> mediaItem.getProduction().getName())),
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
