package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.Attribute;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.MovieDescriptor;
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
public class MovieCollectionSetup extends AbstractCollectionSetup<MovieDescriptor> {
  private static final Type MOVIE = Type.of("MOVIE");

  public MovieCollectionSetup() {
    super(MOVIE);
  }

  @Override
  public Class<?> getLocationClass() {
    return MovieCollectionLocation.class;
  }

  @Override
  protected <T extends MediaDescriptor> T extractDescriptor(MediaStream<T> mediaStream) {
    return Optional.ofNullable(extractDescriptor(mediaStream, DataSource.instance(MOVIE, "TMDB"))).orElseGet(() -> createMovieDescriptor(mediaStream));
  }

  private <T extends MediaDescriptor> T createMovieDescriptor(MediaStream<T> mediaStream) {
    return (T)new MovieDescriptor(
      new Production(
        new ProductionIdentifier(DataSource.instance(MOVIE, "LOCAL"), mediaStream.getStreamPrint().getIdentifier()),
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
      null
    );
  }

  @Override
  protected List<SortOrder<MovieDescriptor>> getAvailableSortOrders() {
    return List.of(
      new SortOrder<MovieDescriptor>("alpha", Comparator.comparing(mediaItem -> mediaItem.getProduction().getName())),
      new SortOrder<MovieDescriptor>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
    );
  }
}
