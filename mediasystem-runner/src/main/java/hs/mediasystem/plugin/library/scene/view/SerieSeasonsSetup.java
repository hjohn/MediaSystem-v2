package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.StreamStateProvider;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.plugin.library.scene.ContextLayout;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
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
public class SerieSeasonsSetup extends AbstractSetup<Season, SerieSeasonsPresentation> {
  @Inject private LocalMediaManager localMediaManager;
  @Inject private SceneNavigator navigator;
  @Inject private ContextLayout contextLayout;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private StreamStateProvider streamStateProvider;
  @Inject private Provider<SerieEpisodesPresentation> serieEpisodesPresentationProvider;

  @Override
  public ObservableList<MediaItem<?>> getItems(SerieSeasonsPresentation presentation) {
    Serie serieDescriptor = (Serie)presentation.mediaItem.get().getData(); // fetchSerieDescriptor(productionItem);

    return FXCollections.observableArrayList(serieDescriptor.getSeasons().stream().map(this::wrap).collect(Collectors.toList()));
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<?>> event, SerieSeasonsPresentation presentation) {
    MediaItem<?> mediaItem = presentation.mediaItem.get();

    navigator.navigateTo(serieEpisodesPresentationProvider.get().set(mediaItem, ((Season)event.getItem().getData()).getNumber()));
    event.consume();
  }

  private int countWatchedStreams(Collection<MediaStream<?>> streams) {
    for(MediaStream<?> stream : streams) {
      if((boolean)streamStateProvider.get(stream.getStreamPrint()).getOrDefault("watched", false)) {
        return 1;
      }
    }

    return 0;
  }

  private MediaItem<Season> wrap(Season data) {
    Set<MediaStream<?>> streams = localMediaManager.find(data.getProduction().getIdentifier());

    return new MediaItem<>(data, streams, countWatchedStreams(streams), streams.isEmpty() ? 0 : 1);
  }

  @Override
  protected Node createContextPanel(SerieSeasonsPresentation presentation) {
    Serie serieDescriptor = ((MediaItem<Serie>)presentation.mediaItem.get()).getData(); // fetchSerieDescriptor(productionItem);

    return contextLayout.create(serieDescriptor.getProduction());
  }

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory cellFactory) {
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
