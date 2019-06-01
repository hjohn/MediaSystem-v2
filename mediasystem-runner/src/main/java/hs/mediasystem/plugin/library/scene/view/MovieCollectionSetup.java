package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.plugin.movies.videolibbaroption.MovieCollectionPresentation;
import hs.mediasystem.scanner.api.MediaType;

import javafx.scene.Node;

import javax.inject.Singleton;

@Singleton
public class MovieCollectionSetup extends AbstractCollectionSetup<Movie, MovieCollectionPresentation> {
  private static final MediaType MOVIE = MediaType.of("MOVIE");

  public MovieCollectionSetup() {
    super(MOVIE);
  }

  @Override
  public Node create(MovieCollectionPresentation presentation) {
    return createView(presentation);
  }
}
