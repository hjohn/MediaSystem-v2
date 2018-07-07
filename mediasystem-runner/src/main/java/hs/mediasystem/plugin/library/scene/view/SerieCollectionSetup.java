package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.Attribute;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.Serie;
import hs.mediasystem.ext.basicmediatypes.Type;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

@Singleton
public class SerieCollectionSetup extends AbstractCollectionSetup<Serie> {
  private static final Type SERIE = Type.of("SERIE");

  public SerieCollectionSetup() {
    super(SERIE);
  }

  @Override
  public Class<?> getLocationClass() {
    return SerieCollectionLocation.class;
  }

  @Override
  protected <T extends MediaDescriptor> T extractDescriptor(MediaStream<T> mediaStream) {
    return Optional.ofNullable(extractDescriptor(mediaStream, DataSource.instance(SERIE, "TMDB"))).orElseGet(() -> createSerieDescriptor(mediaStream));
  }

  private <T extends MediaDescriptor> T createSerieDescriptor(MediaStream<T> mediaStream) {
    return (T)new Serie(
      new Production(
        new ProductionIdentifier(DataSource.instance(SERIE, "LOCAL"), mediaStream.getStreamPrint().getIdentifier()),
        mediaStream.getAttributes().get(Attribute.TITLE),
        null,
        null,
        null,
        null,
        null,
        null,
        Collections.emptyList(),
        Collections.emptyList()
      ),
      Collections.emptyList()
    );
  }

  @Override
  protected List<SortOrder<Serie>> getAvailableSortOrders() {
    return List.of(
      new SortOrder<Serie>("alpha", Comparator.comparing(mediaItem -> mediaItem.getProduction().getName())),
      new SortOrder<Serie>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
    );
  }
}
