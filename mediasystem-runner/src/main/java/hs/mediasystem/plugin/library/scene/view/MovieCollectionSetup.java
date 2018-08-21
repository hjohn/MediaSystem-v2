package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Movie.State;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Type;
import hs.mediasystem.ext.basicmediatypes.scan.Attribute;
import hs.mediasystem.ext.basicmediatypes.scan.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.movies.videolibbaroption.MovieCollectionPresentation;
import hs.mediasystem.util.NaturalLanguage;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javafx.scene.Node;

import javax.inject.Singleton;

@Singleton
public class MovieCollectionSetup extends AbstractCollectionSetup<Movie, MovieCollectionPresentation> {
  private static final Type MOVIE = Type.of("MOVIE");

  public MovieCollectionSetup() {
    super(MOVIE);
  }

  @Override
  protected <T extends MediaDescriptor> Movie extractDescriptor(MediaStream<T> mediaStream) {
    return Optional.ofNullable(extractDescriptor(mediaStream, DataSource.instance(MOVIE, "TMDB"))).orElseGet(() -> createMovieDescriptor(mediaStream));
  }

  private static <T extends MediaDescriptor> Movie createMovieDescriptor(MediaStream<T> mediaStream) {
    return new Movie(
      new ProductionIdentifier(DataSource.instance(MOVIE, "LOCAL"), mediaStream.getStreamPrint().getIdentifier()),
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
      0,
      null,
      State.RELEASED,
      null
    );
  }

  @Override
  protected List<SortOrder<Movie>> getAvailableSortOrders() {
    return List.of(
      new SortOrder<Movie>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL))),
      new SortOrder<Movie>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
    );
  }

  @Override
  public Node create(MovieCollectionPresentation presentation) {
    return createView(presentation);
  }
}
