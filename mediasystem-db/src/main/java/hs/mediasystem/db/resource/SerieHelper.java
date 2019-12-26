package hs.mediasystem.db.resource;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.EpisodeIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Snapshot;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.mediamanager.BasicStreamStore;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.mediamanager.LocalSerie;
import hs.mediasystem.mediamanager.StreamMetaDataStore;
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.Attribute.ChildType;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;
import hs.mediasystem.util.WeightedNgramDistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SerieHelper {
  private static final DataSource LOCAL_RELEASE = DataSource.instance(MediaType.of("RELEASE"), "LOCAL");
  private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
  private static final Set<BasicStream> EMPTY_SET = Collections.emptySet();

  @Inject private BasicStreamStore streamStore;  // Only stores top level items, not children
  @Inject private DescriptorStore descriptorStore;
  @Inject private StreamMetaDataStore metaDataProvider;

  /**
   * Cache containing matches of child identifiers to their streams (for each parent/root identifier).
   * This needs to be flushed periodically as streams can change.  This is no issue as it is mainly
   * to avoid recreating this map every time when many lookups are happening to match a child identifier
   * to its corresponding stream.
   */
  private final Map<Identifier, Map<Identifier, Set<BasicStream>>> streamsByChildIdentifierByRootIdentifier = new HashMap<>();

  public SerieHelper() {
    EXECUTOR_SERVICE.scheduleWithFixedDelay(() -> clearCaches(), 2, 5, TimeUnit.MINUTES);
  }

  public Set<BasicStream> findChildStreams(Identifier rootIdentifier, Identifier childIdentifier) {
    return getChildIdentifierToStreamMap(rootIdentifier).getOrDefault(childIdentifier, EMPTY_SET);
  }

  // This may match multiple episodes if the stream is using an episode range, for example: 2x01-03
  public List<Episode> findChildDescriptors(Serie serie, Attributes childAttributes) {
    String sequence = childAttributes.get(Attribute.SEQUENCE);
    String typeString = childAttributes.get(Attribute.CHILD_TYPE);
    ChildType type = typeString == null ? null : ChildType.valueOf(typeString);

    if(sequence != null && type == ChildType.EPISODE) {
      List<Episode> list = attemptMatch(serie, sequence);  // This will also match specials of the TMDB supported form, with season 0 and an episode number

      if(!list.isEmpty()) {
        return list;
      }
    }

    if(type == null || type == ChildType.SPECIAL) {
      Tuple2<Double, Episode> match = attemptSpecialsMatch(serie, childAttributes.get(Attribute.TITLE), childAttributes.get(Attribute.SUBTITLE), sequence);

      if(match != null) {
        return Collections.singletonList(match.b);
      }
    }

    return Collections.emptyList();
  }

  private void clearCaches() {
    synchronized(streamsByChildIdentifierByRootIdentifier) {
      streamsByChildIdentifierByRootIdentifier.clear();
    }
  }

  private Map<Identifier, Set<BasicStream>> getChildIdentifierToStreamMap(Identifier rootIdentifier) {
    synchronized(streamsByChildIdentifierByRootIdentifier) {
      return streamsByChildIdentifierByRootIdentifier.computeIfAbsent(rootIdentifier, k -> {
        Map<Identifier, Set<BasicStream>> episodeIdentifierToStreams = new LinkedHashMap<>();
        Serie serie = (Serie)descriptorStore.find(rootIdentifier).orElse(null);

        if(serie != null) {
          for(BasicStream parentStream : streamStore.findStreams(rootIdentifier)) {
            List<BasicStream> sortedChildStreams = parentStream.getChildren().stream()
              .sorted((a, b) -> a.getAttributes().<String>get(Attribute.TITLE).compareTo(b.getAttributes().get(Attribute.TITLE)))
              .collect(Collectors.toList());

            for(BasicStream childStream : sortedChildStreams) {
              Attributes attributes = childStream.getAttributes();

              if(!"EXTRA".equals(attributes.get(Attribute.CHILD_TYPE))) {
                List<Episode> result = findChildDescriptors(serie, attributes);

                if(!result.isEmpty()) {
                  result.forEach(e -> episodeIdentifierToStreams.computeIfAbsent(e.getIdentifier(), k2 -> new HashSet<>()).add(childStream));
                  continue;
                }
              }

              // It was identified as an Extra, or did not match any episode, in which case convert it to an Extra:
              episodeIdentifierToStreams.computeIfAbsent(createLocalId(serie, childStream), k2 -> new HashSet<>()).add(childStream);
            }
          }
        }

        return episodeIdentifierToStreams;
      });
    }
  }

  public LocalSerie toLocalSerie(Serie serie) {

    /*
     * For a serie, additional information is added for video files found in the same folder, but
     * which were unable to be matched to a Descriptor that is part of the Serie.  These are "Extras".
     */

    List<Episode> extras = new ArrayList<>();
    int ep = 1;

    Map<Identifier, Set<BasicStream>> episodeToStreamMap = getChildIdentifierToStreamMap(serie.getIdentifier());

    for(Map.Entry<Identifier, Set<BasicStream>> entry : episodeToStreamMap.entrySet()) {
      if(entry.getKey().getDataSource().equals(LOCAL_RELEASE)) {
        // Create an Extra for it:
        BasicStream stream = entry.getValue().iterator().next();
        StreamMetaData metaData = metaDataProvider.find(stream.getId());
        String subtitle = stream.getAttributes().get(Attribute.SUBTITLE);

        extras.add(new Episode(
          createLocalId(serie, stream),
          new Details(
            stream.getAttributes().get(Attribute.TITLE) + (subtitle == null ? "" : ": " + subtitle),
            null,
            null,
            Optional.ofNullable(metaData).map(StreamMetaData::getSnapshots).filter(list -> list.size() > 1).map(list -> list.get(1)).map(Snapshot::getImageUri).orElse(null),
            null
          ),
          null,
          null,
          -1,
          ep++,
          Collections.emptyList()
        ));
      }
    }

    return new LocalSerie(
      serie.getIdentifier(),
      serie.getDetails(),
      serie.getReception(),
      serie.getLanguages(),
      serie.getGenres(),
      serie.getKeywords(),
      serie.getState(),
      serie.getLastAirDate(),
      serie.getPopularity(),
      serie.getSeasons(),
      serie.getRelatedIdentifiers(),
      extras
    );
  }

  private static LocalEpisodeIdentifier createLocalId(Serie serie, BasicStream stream) {
    return new LocalEpisodeIdentifier(LOCAL_RELEASE, serie.getIdentifier().getId() + "/" + stream.getId().asInt(), serie.getIdentifier());
  }

  private static class LocalEpisodeIdentifier extends EpisodeIdentifier {
    private final Identifier rootIdentifier;

    public LocalEpisodeIdentifier(DataSource dataSource, String id, Identifier rootIdentifier) {
      super(dataSource, id);

      this.rootIdentifier = rootIdentifier;
    }

    @Override
    public Identifier getRootIdentifier() {
      return rootIdentifier;
    }
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

  private static Tuple2<Double, Episode> attemptSpecialsMatch(Serie serie, String title, String subtitle, String sequence) {
    return serie.findSeason(0).map(season -> {
      Episode bestEpisode = null;
      double bestMatch = 0;

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
