package hs.mediasystem.ext.tmdb;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.api.Discovery;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.tmdb.identifier.EpisodeIdentifier;
import hs.mediasystem.ext.tmdb.identifier.MovieIdentifier;
import hs.mediasystem.ext.tmdb.identifier.SerieIdentifier;
import hs.mediasystem.util.Attributes;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbIdentificationService implements IdentificationService {
  @Inject private MovieIdentifier movieIdentifier;
  @Inject private SerieIdentifier serieIdentifier;
  @Inject private EpisodeIdentifier episodeIdentifier;

  @Override
  public String getName() {
    return "TMDB";
  }

  @Override
  public Optional<Identification> identify(Discovery discovery, WorkDescriptor parent) throws IOException {
    MediaType mediaType = discovery.mediaType();
    Attributes attributes = discovery.attributes();

    if(mediaType == MediaType.SERIE) {
      return serieIdentifier.identify(attributes);
    }
    if(mediaType == MediaType.EPISODE) {
      return episodeIdentifier.identify(attributes, parent);
    }
    if(mediaType == MediaType.MOVIE) {
      return movieIdentifier.identify(attributes);
    }

    throw new IllegalArgumentException("Unsupported media type: " + mediaType);
  }
}
