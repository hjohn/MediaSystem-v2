package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.Attribute.ChildType;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;
import hs.mediasystem.util.WeightedNgramDistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

// Note: this bridges between attributes and domain model to match a Serie's child streams to specific Episodes; not sure where this should be from a dependency point of view
@Singleton
public class EpisodeMatcher {

  @SuppressWarnings("static-method")
  public List<Episode> attemptMatch(Serie serie, Attributes attributes) {
    String sequence = attributes.get(Attribute.SEQUENCE);
    String typeString = attributes.get(Attribute.CHILD_TYPE);
    ChildType type = typeString == null ? null : ChildType.valueOf(typeString);

    if(sequence != null && type == ChildType.EPISODE) {
      List<Episode> list = attemptMatch(serie, sequence);  // This will also match specials of the TMDB supported form, with season 0 and an episode number

      if(!list.isEmpty()) {
        return list;
      }
    }

    if(type == null || type == ChildType.SPECIAL) {
      Tuple2<Double, Episode> match = attemptSpecialsMatch(serie, attributes.get(Attribute.TITLE), attributes.get(Attribute.SUBTITLE), sequence);

      if(match != null) {
        return Collections.singletonList(match.b);
      }
    }

    return null;
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

      Season season = serie.findSeason(seasonNumber);

      if(season != null) {
        for(int i = Integer.parseInt(numbers[0]); i <= Integer.parseInt(numbers[numbers.length - 1]); i++) {
          Episode episode = season.findEpisode(i);

          if(episode != null) {
            list.add(episode);
          }
        }
      }
    }

    return list;
  }

  private static Tuple2<Double, Episode> attemptSpecialsMatch(Serie serie, String title, String subtitle, String sequence) {
    Season season = serie.findSeason(0);
    Episode bestEpisode = null;
    double bestMatch = 0;

    if(season != null) {
      for(String joinedString : combinations(title, subtitle, sequence)) {

        for(Episode episode : season.getEpisodes()) {
          String name = episode.getDetails().getName();
          double match = WeightedNgramDistance.calculate(name, joinedString);

          if(match > 0.5 && match > bestMatch) {
            bestMatch = match;
            bestEpisode = episode;
          }
        }
      }
    }

    return bestEpisode == null ? null : Tuple.of(bestMatch, bestEpisode);
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
