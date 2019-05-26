package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.EpisodeIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Snapshot;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

// TODO move to front-end code?
@Singleton
public class MediaService {
  private static final DataSource LOCAL_RELEASE = DataSource.instance(MediaType.of("RELEASE"), "LOCAL");
  private static final Set<BasicStream> EMPTY_SET = Collections.emptySet();

  @Inject private BasicStreamStore streamStore;  // Only stores top level items, not children
  @Inject private DescriptorStore descriptorStore;
  @Inject private LocalMediaManager localMediaManager;  // TODO doesn't belong here
  @Inject private StreamMetaDataProvider metaDataProvider;
  @Inject private EpisodeMatcher episodeMatcher;  // May need further generalization

  private final Map<Identifier, Map<Identifier, Set<BasicStream>>> streamsByChildIdentifierByRootIdentifier = new HashMap<>();

  public Set<BasicStream> findStreams(Identifier identifier) {
    Identifier rootIdentifier = identifier.getRootIdentifier();

    if(rootIdentifier != null) {  // Special case, child identifiers are not in StreamStore, get from map
      return getChildIdentifierToStreamMap(rootIdentifier).getOrDefault(identifier, EMPTY_SET);
    }

    return streamStore.findStreams(identifier);
  }

  private Map<Identifier, Set<BasicStream>> getChildIdentifierToStreamMap(Identifier rootIdentifier) {
    return streamsByChildIdentifierByRootIdentifier.computeIfAbsent(rootIdentifier, k -> {
      Map<Identifier, Set<BasicStream>> episodeIdentifierToStreams = new LinkedHashMap<>();
      Serie serie = (Serie)descriptorStore.get(rootIdentifier);

      if(serie != null) {
        for(BasicStream parentStream : streamStore.findStreams(rootIdentifier)) {
          List<BasicStream> sortedChildStreams = parentStream.getChildren().stream()
            .sorted((a, b) -> a.getAttributes().<String>get(Attribute.TITLE).compareTo(b.getAttributes().get(Attribute.TITLE)))
            .collect(Collectors.toList());

          for(BasicStream childStream : sortedChildStreams) {
            Attributes attributes = childStream.getAttributes();

            if(!"EXTRA".equals(attributes.get(Attribute.CHILD_TYPE))) {
              List<Episode> result = episodeMatcher.attemptMatch(serie, attributes);

              if(result != null) {
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

  public MediaIdentification reidentify(StreamID streamId) {
    return localMediaManager.reidentifyStream(streamId);
  }

  public <D extends MediaDescriptor> List<D> findAllByType(MediaType type, List<String> dataSourceNames) {
    return findAllByType(type, null, dataSourceNames);
  }

  public <D extends MediaDescriptor> List<D> findAllByType(MediaType type, String tag, List<String> dataSourceNames) {
    List<D> descriptors = new ArrayList<>();

    for(Map.Entry<BasicStream, Map<Identifier, Identification>> entry : streamStore.findIdentifiersByStreams(type, tag).entrySet()) {
      @SuppressWarnings("unchecked")
      D descriptor = (D)findDataSourcePreferredDescriptor(entry.getValue().keySet(), dataSourceNames);

      if(descriptor != null) {     // TODO should generate LOCAL entries on demand for Streams without any Descriptor
        descriptors.add(descriptor);  // TODO this can add duplicate descriptors if there are 2 streams that identify to same thing
      }
    }

    return descriptors;
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
      extras
    );
  }

  private static LocalEpisodeIdentifier createLocalId(Serie serie, BasicStream stream) {
    return new LocalEpisodeIdentifier(LOCAL_RELEASE, serie.getIdentifier().getId() + "/" + stream.getId().asInt(), serie.getIdentifier());
  }

  public Identification getIdentification(StreamID streamId, List<String> dataSourceNames) {
    Identification bestIdentification = null;
    int bestIndex = Integer.MAX_VALUE;

    Map<Identifier, Identification> identifications = streamStore.findIdentifications(streamId);

    if(identifications != null) {
      for(Map.Entry<Identifier, Identification> entry : identifications.entrySet()) {
        if(entry.getValue() != null) {
          int index = dataSourceNames.indexOf(entry.getKey().getDataSource().getName());

          if(index >= 0 && index < bestIndex) {
            bestIndex = index;
            bestIdentification = entry.getValue();
          }
        }
      }
    }

    return bestIdentification;
  }

  private MediaDescriptor findDataSourcePreferredDescriptor(Set<Identifier> set, List<String> dataSourceNames) {
    MediaDescriptor bestDescriptor = null;
    int bestIndex = Integer.MAX_VALUE;

    for(Identifier identifier : set) {
      MediaDescriptor descriptor = descriptorStore.get(identifier);

      if(descriptor != null) {
        int index = dataSourceNames.indexOf(identifier.getDataSource().getName());

        if(index >= 0 && index < bestIndex) {
          bestIndex = index;
          bestDescriptor = descriptor;
        }
      }
    }

    return bestDescriptor;
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
}
