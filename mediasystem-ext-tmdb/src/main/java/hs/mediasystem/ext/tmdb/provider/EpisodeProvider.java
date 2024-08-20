package hs.mediasystem.ext.tmdb.provider;

import hs.mediasystem.api.datasource.domain.Episode;
import hs.mediasystem.api.datasource.domain.Serie;
import hs.mediasystem.api.datasource.domain.Serie.Season;
import hs.mediasystem.domain.work.Context;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EpisodeProvider implements MediaProvider<Episode> {
  @Inject private SerieProvider serieProvider;

  @Override
  public Optional<Episode> provide(String key) throws IOException {
    String[] parts = key.split("/");

    if(parts.length != 3) {
      throw new IllegalArgumentException("key must be an episode key: " + key);
    }

    return serieProvider.provide(parts[0])
      .flatMap(serie -> serie.getSeasons().stream()
        .map(Season::episodes)
        .flatMap(Collection::stream)
        .filter(ep -> ep.id().getKey().equals(key))
        .findFirst()
        .map(ep -> toEpisode(serie, ep))
      );
  }

  private static Episode toEpisode(Serie serie, Serie.Episode episode) {
    return new Episode(
      episode.id(),
      episode.details(),
      episode.reception(),
      new Context(serie.getId(), serie.getTitle(), serie.getCover(), serie.getBackdrop()),
      episode.duration(),
      episode.seasonNumber(),
      episode.number(),
      episode.personRoles()
    );
  }
}
