package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.scan.Attribute;
import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.plugin.library.scene.ContextLayout;
import hs.mediasystem.plugin.library.scene.MediaGridView;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class SerieEpisodesSetup extends AbstractSetup<Episode, SerieEpisodesPresentation> {
  @Inject private LocalMediaManager localMediaManager;
  @Inject private ContextLayout contextLayout;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private StreamStateService streamStateService;
  @Inject private Provider<ProductionDetailPresentation> detailPresentationProvider;

  @Override
  public ObservableList<MediaItem<Episode>> getItems(SerieEpisodesPresentation presentation) {
    SerieEpisodesPresentation p = presentation;
    MediaItem<Serie> mediaItem = p.mediaItem.get();
    Serie serieDescriptor = mediaItem.getData(); // fetchSerieDescriptor(productionItem);

    Map<Integer, Map<Integer, Set<MediaStream<?>>>> serieIndex = createSerieIndex(mediaItem);

    return FXCollections.observableArrayList(serieDescriptor.getSeasons().stream().filter(s -> s.getNumber() == p.seasonNumber.get()).findFirst().map(Season::getEpisodes).stream().flatMap(List::stream).map(s -> wrap(mediaItem, s, serieIndex)).collect(Collectors.toList()));
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<Episode>> event, SerieEpisodesPresentation presentation) {
    Event.fireEvent(event.getTarget(), NavigateEvent.to(detailPresentationProvider.get().set(event.getItem())));
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

  private MediaItem<Episode> wrap(MediaItem<Serie> serieItem, Episode data, Map<Integer, Map<Integer, Set<MediaStream<?>>>> streamsByEpisodeBySeason) {
    Set<MediaStream<?>> streams = Optional.ofNullable(streamsByEpisodeBySeason.get(data.getSeasonNumber())).map(m -> m.get(data.getNumber())).orElse(Collections.emptySet());

    return new MediaItem<>(
      data,
      serieItem,
      streams,
      countWatchedStreams(streams),
      streams.isEmpty() ? 0 : 1
    );
  }

  @Override
  protected Node createContextPanel(SerieEpisodesPresentation presentation) {
    MediaItem<Serie> mediaItem = presentation.mediaItem.get();
    Serie serieDescriptor = mediaItem.getData(); // fetchSerieDescriptor(productionItem);

    return contextLayout.create(serieDescriptor, presentation.seasonNumber.get());
  }

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<Episode> cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
    cellFactory.setSequenceNumberExtractor(item -> Optional.ofNullable(item.getData().getNumber()).map(i -> "" + i).orElse(null));
  }

  @Override
  protected void configureGridView(MediaGridView<MediaItem<Episode>> gridView) {
    super.configureGridView(gridView);

    gridView.visibleRows.set(4);
    gridView.visibleColumns.set(3);
  }

  private Map<Integer, Map<Integer, Set<MediaStream<?>>>> createSerieIndex(MediaItem<?> serieItem) {
    Set<MediaStream<?>> episodeStreams = localMediaManager.findChildren(serieItem.getStreams().iterator().next().getUri());

    Map<Integer, Map<Integer, Set<MediaStream<?>>>> streamsByEpisodeBySeason = new HashMap<>();

    for(MediaStream<?> stream : episodeStreams) {
      String sequenceAttribute = (String)stream.getAttributes().get(Attribute.SEQUENCE);

      if(sequenceAttribute != null) {
        String[] parts = sequenceAttribute.split(",");

        if(parts.length == 2) {
          int seasonNumber = Integer.parseInt(parts[0]);
          String[] numbers = parts[1].split("-");

          for(int i = Integer.parseInt(numbers[0]); i <= Integer.parseInt(numbers[numbers.length - 1]); i++) {
            streamsByEpisodeBySeason.computeIfAbsent(seasonNumber, k -> new HashMap<>()).computeIfAbsent(i, k -> new HashSet<>()).add(stream);
          }
        }
        else {
          int episodeNumber = Integer.parseInt(parts[0]);

          streamsByEpisodeBySeason.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(episodeNumber, k -> new HashSet<>()).add(stream);
        }
      }
      else {
        streamsByEpisodeBySeason.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(0, k -> new HashSet<>()).add(stream);
      }
    }

    return streamsByEpisodeBySeason;
  }

  @Override
  protected List<SortOrder<Episode>> getAvailableSortOrders() {
    return List.of(
      new SortOrder<Episode>("episode-number", Comparator.comparing(mediaItem -> mediaItem.getData().getNumber()))
    );
  }

  @Override
  protected List<Filter<Episode>> getAvailableFilters() {
    return List.of(new Filter<Episode>("none", mi -> true));
  }

  @Override
  protected boolean showViewed() {
    return true;
  }

  @Override
  public Node create(SerieEpisodesPresentation presentation) {
    return createView(presentation);
  }
}
