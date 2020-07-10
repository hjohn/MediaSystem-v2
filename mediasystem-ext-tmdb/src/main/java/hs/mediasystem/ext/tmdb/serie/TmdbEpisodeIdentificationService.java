package hs.mediasystem.ext.tmdb.serie;

import hs.ddif.annotations.PluginScoped;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute.ChildType;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractIdentificationService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;
import hs.mediasystem.util.WeightedNgramDistance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@PluginScoped
public class TmdbEpisodeIdentificationService extends AbstractIdentificationService {

  public TmdbEpisodeIdentificationService() {
    super(DataSources.TMDB_EPISODE);
  }

  @Override
  public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
    return findChildDescriptors((Serie)parent, streamable.getAttributes());
  }

  private static Optional<Identification> findChildDescriptors(Serie serie, Attributes childAttributes) {
    String sequence = childAttributes.get(Attribute.SEQUENCE);
    String typeString = childAttributes.get(Attribute.CHILD_TYPE);
    ChildType type = typeString == null ? null : ChildType.valueOf(typeString);

    if(sequence != null && type == ChildType.EPISODE) {
      List<Episode> list = attemptMatch(serie, sequence);  // This will also match specials of the TMDB supported form, with season 0 and an episode number

      if(!list.isEmpty()) {
        return Optional.of(new Identification(list.stream().map(Episode::getIdentifier).collect(Collectors.toList()), new Match(Type.DERIVED, 1.0f, Instant.now())));
      }
    }

    if(type == null || type == ChildType.SPECIAL) {
      Tuple2<Float, Episode> match = attemptSpecialsMatch(serie, childAttributes.get(Attribute.TITLE), childAttributes.get(Attribute.SUBTITLE), sequence);

      if(match != null) {
        return Optional.of(new Identification(Collections.singletonList(match.b.getIdentifier()), new Match(Type.NAME, match.a, Instant.now())));
      }
    }

    return Optional.empty();
  }

  private static List<Episode> attemptMatch(Serie serie, String sequence) {
    List<Episode> list = new ArrayList<>();
    String[] parts = sequence.split(",");

    /*
     * Sequence can be in three forms:
     *
     * "x,y" = season x, episode y
     * ",y" = season 1 (or absolute ordering), episode y
     * "x" = season x (useless, ignore)
     */

    if(parts.length == 2) {
      int seasonNumber = parts[0].isEmpty() ? 1 : Integer.parseInt(parts[0]);  // Assume season is 1 if consists of two parts but season empty
      String[] numbers = parts[1].split("-");

      serie.findSeason(seasonNumber).ifPresent(season -> {
        for(int i = Integer.parseInt(numbers[0]); i <= Integer.parseInt(numbers[numbers.length - 1]); i++) {
          Episode episode = season.findEpisode(i);

          if(episode != null) {
            list.add(episode);
          }
        }
      });
    }

    return list;
  }

  private static Tuple2<Float, Episode> attemptSpecialsMatch(Serie serie, String title, String subtitle, String sequence) {
    return serie.findSeason(0).map(season -> {
      Episode bestEpisode = null;
      float bestMatch = 0;

      for(String joinedString : combinations(title, subtitle, sequence)) {
        for(Episode episode : season.getEpisodes()) {
          String name = episode.getDetails().getTitle();
          float match = (float)WeightedNgramDistance.calculate(name, joinedString);

          if(match > 0.5 && match > bestMatch) {
            bestMatch = match;
            bestEpisode = episode;
          }
        }
      }

      return bestEpisode == null ? null : Tuple.of(bestMatch, bestEpisode);
    })
    .orElse(null);
  }

  private static List<String> combinations(String title, String subtitle, String sequence) {
    List<String> combinations = new ArrayList<>();

    if(subtitle != null && !subtitle.isEmpty()) {
      combinations.add(title + " " + subtitle);
      combinations.add(subtitle);
    }
    else if(sequence == null || sequence.isEmpty()) {
      combinations.add(title);
    }

    return combinations;
  }
}
