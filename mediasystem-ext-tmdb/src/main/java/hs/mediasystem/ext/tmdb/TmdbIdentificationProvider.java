package hs.mediasystem.ext.tmdb;

import hs.mediasystem.api.datasource.domain.Identification;
import hs.mediasystem.api.datasource.domain.Serie;
import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.tmdb.identifier.EpisodeIdentifier;
import hs.mediasystem.ext.tmdb.identifier.MovieIdentifier;
import hs.mediasystem.ext.tmdb.identifier.SerieIdentifier;
import hs.mediasystem.util.Attributes;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbIdentificationProvider implements IdentificationProvider {
  @Inject private MovieIdentifier movieIdentifier;
  @Inject private SerieIdentifier serieIdentifier;
  @Inject private EpisodeIdentifier episodeIdentifier;

  @Override
  public String getName() {
    return "TMDB";
  }

  @Override
  public Optional<Identification> identify(Discovery discovery) throws IOException {
    MediaType mediaType = discovery.mediaType();
    Attributes attributes = discovery.attributes();

    if(mediaType == MediaType.SERIE) {
      return serieIdentifier.identify(attributes);
    }
    if(mediaType == MediaType.MOVIE) {
      return movieIdentifier.identify(attributes);
    }

    throw new IllegalArgumentException("Unsupported media type: " + mediaType);
  }

  @Override
  public Identification identifyChild(Discovery discovery, Identification parent) {
    MediaType mediaType = discovery.mediaType();
    Attributes attributes = discovery.attributes();

    if(mediaType == MediaType.EPISODE) {
      return episodeIdentifier.identify(attributes, (Serie)parent.releases().getFirst())
        .orElse(IdentificationProvider.MINIMAL_PROVIDER.identifyChild(discovery, parent));
    }

    throw new IllegalArgumentException("Unsupported media type: " + mediaType);
  }
}
