package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Type;
import hs.mediasystem.ext.basicmediatypes.scan.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.scan.MediaRecord;
import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.plugin.library.scene.MediaGridView;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.runner.ImageHandleFactory;
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

public abstract class AbstractCollectionSetup<T, P extends GridViewPresentation> extends AbstractSetup<T, P> {
  private static final String SYSTEM = "MediaSystem:Library:Collection";

  private final Type type;

  @Inject private LocalMediaManager localMediaManager;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private StreamStateService streamStateService;
  @Inject private PresentationLoader presentationLoader;
  @Inject private SettingsStore settingsStore;
  @Inject private ProductionPresentation.Factory productionPresentationFactory;

  public AbstractCollectionSetup(Type type) {
    this.type = type;
  }

  @Override
  public ObservableList<MediaItem<T>> getItems(GridViewPresentation presentation) {
    Collection<MediaStream<MediaDescriptor>> mediaStreams = localMediaManager.findAllByType(type);

    return createProductionItems(mediaStreams, this::extractDescriptor);
  }

  protected abstract <M extends MediaDescriptor> T extractDescriptor(MediaStream<M> mediaStream);

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<T> cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
  }

  private <D extends MediaDescriptor> int countWatchedStreams(MediaStream<D> stream) {
    if(streamStateService.isWatched(stream.getStreamPrint())) {
      return 1;
    }

    return 0;
  }

  private <D extends MediaDescriptor> ObservableList<MediaItem<T>> createProductionItems(Collection<MediaStream<D>> streams, Function<MediaStream<D>, T> mapper) {
    return FXCollections.observableArrayList(streams.stream().map(s -> new MediaItem<>(
      mapper.apply(s),
      null,
      Set.of(s),
      countWatchedStreams(s),
      1
    )).collect(Collectors.toList()));
  }

  protected <D extends MediaDescriptor> T extractDescriptor(MediaStream<D> mediaStream, DataSource dataSource) {
    for(Identifier identifier : mediaStream.getMediaRecords().keySet()) {
      MediaRecord<D> record = mediaStream.getMediaRecords().get(identifier);

      if(identifier.getDataSource() == dataSource) {
        return (T)record.getMediaDescriptor();
      }
    }

    return null;
  }

  @Override
  protected String getSelectedId(P presentation) {
    String id = settingsStore.getSetting(SYSTEM, "last-selected:" + type);

    return id == null ? super.getSelectedId(presentation) : id;
  }

  @Override
  protected void configureGridView(MediaGridView<MediaItem<T>> gridView) {
    super.configureGridView(gridView);

    gridView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> {
      if(current != null) {
        settingsStore.storeSetting(SYSTEM, "last-selected:" + type, current.getId());
      }
    });
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<T>> event, P presentation) {
    presentationLoader.loadAndNavigate(event, () -> productionPresentationFactory.create(event.getItem()));
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
