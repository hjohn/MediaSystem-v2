package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.db.StreamStateProvider;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.MediaRecord;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.Type;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.plugin.library.scene.MediaGridView;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
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
import javax.inject.Provider;

public abstract class AbstractCollectionSetup<T, P extends GridViewPresentation> extends AbstractSetup<T, P> {
  private static final String SYSTEM = "MediaSystem:Library:Collection";

  private final Type type;

  @Inject private LocalMediaManager localMediaManager;
  @Inject private SceneNavigator navigator;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private StreamStateProvider streamStateProvider;
  @Inject private Provider<ProductionDetailPresentation> detailPresentationProvider;
  @Inject private Provider<ProductionPresentation> productionPresentationProvider;
  @Inject private SettingsStore settingsStore;

  public AbstractCollectionSetup(Type type) {
    this.type = type;
  }

  @Override
  public ObservableList<MediaItem<?>> getItems(GridViewPresentation presentation) {
    Collection<MediaStream<MediaDescriptor>> mediaStreams = localMediaManager.findAllByType(type);

    return (ObservableList<MediaItem<?>>)(ObservableList<?>)createProductionItems(mediaStreams, this::extractDescriptor);
  }

  protected abstract <M extends MediaDescriptor> M extractDescriptor(MediaStream<M> mediaStream);

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
  }

  private <D extends MediaDescriptor> int countWatchedStreams(MediaStream<D> stream) {
    if((boolean)streamStateProvider.get(stream.getStreamPrint()).getOrDefault("watched", false)) {
      return 1;
    }

    return 0;
  }

  private <D extends MediaDescriptor> ObservableList<MediaItem<D>> createProductionItems(Collection<MediaStream<D>> streams, Function<MediaStream<D>, D> mapper) {
    return FXCollections.observableArrayList(streams.stream().map(s -> new MediaItem<>(
      mapper.apply(s),
      Set.of(s),
      countWatchedStreams(s),
      1
    )).collect(Collectors.toList()));
  }

  protected <D extends MediaDescriptor> D extractDescriptor(MediaStream<D> mediaStream, DataSource dataSource) {
    for(Identifier identifier : mediaStream.getMediaRecords().keySet()) {
      MediaRecord<D> record = mediaStream.getMediaRecords().get(identifier);

      if(identifier.getDataSource() == dataSource) {
        return record.getMediaDescriptor();
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
  protected void configureGridView(MediaGridView<MediaItem<?>> gridView) {
    super.configureGridView(gridView);

    gridView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> settingsStore.storeSetting(SYSTEM, "last-selected:" + type, current.getId()));
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<?>> event, P presentation) {
    //navigator.navigateTo(detailPresentationProvider.get().set(event.getItem()));
    navigator.navigateTo(productionPresentationProvider.get().set(event.getItem()));
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
