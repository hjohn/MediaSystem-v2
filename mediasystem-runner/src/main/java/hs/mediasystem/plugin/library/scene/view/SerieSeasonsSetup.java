package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.plugin.library.scene.ContextLayout;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class SerieSeasonsSetup extends AbstractSetup<Season, SerieSeasonsPresentation> {
  @Inject private LocalMediaManager localMediaManager;
  @Inject private ContextLayout contextLayout;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private StreamStateService streamStateService;
  @Inject private Provider<SerieEpisodesPresentation> serieEpisodesPresentationProvider;

  @Override
  public ObservableList<MediaItem<Season>> getItems(SerieSeasonsPresentation presentation) {
    Serie serieDescriptor = presentation.mediaItem.get().getData(); // fetchSerieDescriptor(productionItem);

    return FXCollections.observableArrayList(serieDescriptor.getSeasons().stream().map(season -> wrap(presentation.mediaItem.get(), season)).collect(Collectors.toList()));
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<Season>> event, SerieSeasonsPresentation presentation) {
    MediaItem<Serie> mediaItem = presentation.mediaItem.get();

    Event.fireEvent(event.getTarget(), NavigateEvent.to(serieEpisodesPresentationProvider.get().set(mediaItem, event.getItem().getData().getNumber())));
    event.consume();
  }

  private int countWatchedStreams(Collection<MediaStream<?>> streams) {
    for(MediaStream<?> stream : streams) {
      if(streamStateService.isWatched(stream.getStreamPrint())) {
        return 1;
      }
    }

    return 0;
  }

  private MediaItem<Season> wrap(MediaItem<Serie> serieItem, Season data) {
    Set<MediaStream<?>> streams = localMediaManager.find(data.getIdentifier());

    return new MediaItem<>(data, serieItem, streams, countWatchedStreams(streams), streams.isEmpty() ? 0 : 1);
  }

  @Override
  protected Node createContextPanel(SerieSeasonsPresentation presentation) {
    Serie serie = presentation.mediaItem.get().getData(); // fetchSerieDescriptor(productionItem);

    return contextLayout.create(serie);
  }

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<Season> cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
  }

  @Override
  protected List<SortOrder<Season>> getAvailableSortOrders() {
    return List.of(
      new SortOrder<Season>("season-number", Comparator.comparing(mediaItem -> mediaItem.getData().getNumber()))
    );
  }

  @Override
  protected List<Filter<Season>> getAvailableFilters() {
    return List.of(new Filter<Season>("none", mi -> true));
  }

  @Override
  protected boolean showViewed() {
    return true;
  }

  @Override
  public Node create(SerieSeasonsPresentation presentation) {
    return createView(presentation);
  }
}
