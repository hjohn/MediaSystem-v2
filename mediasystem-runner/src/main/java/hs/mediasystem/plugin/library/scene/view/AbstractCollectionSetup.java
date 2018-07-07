package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.StreamStateProvider;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.MediaRecord;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.Type;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.plugin.library.scene.LibraryLocation;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.runner.SceneNavigator;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;

public abstract class AbstractCollectionSetup<T> extends AbstractSetup<T> {
  private final Type type;

  @Inject private LocalMediaManager localMediaManager;
  @Inject private SceneNavigator navigator;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private StreamStateProvider streamStateProvider;

  public AbstractCollectionSetup(Type type) {
    this.type = type;
  }

  @Override
  public ObservableList<MediaItem<?>> getItems(LibraryLocation location) {
    Collection<MediaStream<MediaDescriptor>> mediaStreams = localMediaManager.findAllByType(type);

    return (ObservableList<MediaItem<?>>)(ObservableList<?>)createProductionItems(mediaStreams, this::extractDescriptor);
  }

  protected abstract <T extends MediaDescriptor> T extractDescriptor(MediaStream<T> mediaStream);

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
  }

  private <T extends MediaDescriptor> int countWatchedStreams(MediaStream<T> stream) {
    if((boolean)streamStateProvider.get(stream.getStreamPrint()).getOrDefault("watched", false)) {
      return 1;
    }

    return 0;
  }

  private <T extends MediaDescriptor> ObservableList<MediaItem<T>> createProductionItems(Collection<MediaStream<T>> streams, Function<MediaStream<T>, T> mapper) {
    return FXCollections.observableArrayList(streams.stream().map(s -> new MediaItem<>(
      mapper.apply(s),
      Set.of(s),
      countWatchedStreams(s),
      1
    )).collect(Collectors.toList()));
  }

  protected <T extends MediaDescriptor> T extractDescriptor(MediaStream<T> mediaStream, DataSource dataSource) {
    for(Identifier identifier : mediaStream.getMediaRecords().keySet()) {
      MediaRecord<T> record = mediaStream.getMediaRecords().get(identifier);

      if(identifier.getDataSource() == dataSource) {
        return record.getMediaDescriptor();
      }
    }

    return null;
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<?>> event, LibraryLocation location) {
    navigator.go(new DetailLocation(event.getItem()));
    event.consume();
  }

  @Override
  protected List<Filter<T>> getAvailableFilters() {
    return List.of(
      new Filter<T>("none", mi -> true),
      new Filter<T>("released-recently", mi -> Optional.ofNullable(mi.getProduction().getDate()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
    );
  }

  @Override
  protected boolean showViewed() {
    return true;
  }
}
