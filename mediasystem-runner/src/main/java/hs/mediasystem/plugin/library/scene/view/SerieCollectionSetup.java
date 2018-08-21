package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.Serie.State;
import hs.mediasystem.ext.basicmediatypes.domain.Type;
import hs.mediasystem.ext.basicmediatypes.scan.Attribute;
import hs.mediasystem.ext.basicmediatypes.scan.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.util.NaturalLanguage;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javafx.scene.Node;

import javax.inject.Singleton;

@Singleton
public class SerieCollectionSetup extends AbstractCollectionSetup<Serie, SerieCollectionPresentation> {
  private static final Type SERIE = Type.of("SERIE");

  public SerieCollectionSetup() {
    super(SERIE);
  }

  @Override
  protected <T extends MediaDescriptor> Serie extractDescriptor(MediaStream<T> mediaStream) {
    return Optional.ofNullable(extractDescriptor(mediaStream, DataSource.instance(SERIE, "TMDB"))).orElseGet(() -> createSerieDescriptor(mediaStream));
  }

  private static <T extends MediaDescriptor> Serie createSerieDescriptor(MediaStream<T> mediaStream) {
    return new Serie(
      new ProductionIdentifier(DataSource.instance(SERIE, "LOCAL"), mediaStream.getStreamPrint().getIdentifier()),
      new Details(
        mediaStream.getAttributes().get(Attribute.TITLE),
        null,
        null,
        null,
        null
      ),
      null,
      null,
      Collections.emptyList(),
      Collections.emptyList(),
      State.ENDED,
      null,
      0,
      Collections.emptyList()
    );
  }

  @Override
  protected List<SortOrder<Serie>> getAvailableSortOrders() {
    return List.of(
      new SortOrder<Serie>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL))),
      new SortOrder<Serie>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
    );
  }

  @Override
  public Node create(SerieCollectionPresentation presentation) {
    return createView(presentation);
  }
}
