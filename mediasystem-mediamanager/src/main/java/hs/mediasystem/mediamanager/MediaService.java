package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.EpisodeIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.Serie.State;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Snapshot;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Tuple.Tuple2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

  @Inject private StreamStore store;
  @Inject private LocalMediaManager localMediaManager;
  @Inject private StreamMetaDataProvider metaDataProvider;

  public Set<BasicStream> findStreams(Identifier identifier) {
    if(identifier.getDataSource().equals(LOCAL_RELEASE)) {
      BasicStream stream = store.findStream(new StreamID(Integer.parseInt(identifier.substring(7).getId())));

      return stream == null ? Collections.emptySet() : Collections.singleton(stream);
    }

    return store.findStreams(identifier);
  }

  public MediaIdentification reidentify(StreamID streamId) {
    return localMediaManager.reidentifyStream(streamId);
  }

  public <D extends MediaDescriptor> List<D> findAllByType(MediaType type, List<String> dataSourceNames) {
    List<D> descriptors = new ArrayList<>();

    for(Map.Entry<BasicStream, Map<Identifier, Tuple2<Identification, MediaDescriptor>>> entry : store.findAllDescriptorsAndIdentifications(type).entrySet()) {
      @SuppressWarnings("unchecked")
      D descriptor = (D)findDataSourcePreferredDescriptor(entry.getValue().values(), dataSourceNames);

      if(descriptor != null) {     // TODO should generate LOCAL entries on demand for Streams without any Descriptor
        descriptors.add(descriptor);  // TODO this can add duplicate descriptors if there are 2 streams that identify to same thing
      }
    }

    return descriptors;
  }

  public <D extends MediaDescriptor> List<D> findAllByType(MediaType type, String tag, List<String> dataSourceNames) {
    List<D> descriptors = new ArrayList<>();

    for(Map.Entry<BasicStream, Map<Identifier, Tuple2<Identification, MediaDescriptor>>> entry : store.findAllDescriptorsAndIdentifications(type, tag).entrySet()) {
      @SuppressWarnings("unchecked")
      D descriptor = (D)findDataSourcePreferredDescriptor(entry.getValue().values(), dataSourceNames);

      if(descriptor != null) {     // TODO should generate LOCAL entries on demand for Streams without any Descriptor
        descriptors.add(descriptor);  // TODO this can add duplicate descriptors if there are 2 streams that identify to same thing
      }
    }

    return descriptors;
  }

  public LocalSerie toLocalSerie(BasicStream serieStream, Serie serie) {

    /*
     * For a serie, additional information is added for video files found in the same folder, but
     * which were unable to be matched to a Descriptor that is part of the Serie.  These are "Extras".
     */

    List<Episode> extras = new ArrayList<>();
    int ep = 1;

    List<BasicStream> sortedChildStreams = serieStream.getChildren().stream()
      .sorted((a, b) -> a.getAttributes().<String>get(Attribute.TITLE).compareTo(b.getAttributes().get(Attribute.TITLE)))
      .collect(Collectors.toList());

    for(BasicStream stream : sortedChildStreams) {
      if(store.findIdentifiers(stream.getId()).isEmpty()) {
        // Create an Extra for it:
        StreamMetaData metaData = metaDataProvider.find(stream.getId());
        String subtitle = stream.getAttributes().get(Attribute.SUBTITLE);

        extras.add(new Episode(
          new EpisodeIdentifier(LOCAL_RELEASE, "parent/" + stream.getId().asInt()),
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

  public Identification getIdentification(StreamID streamId, List<String> dataSourceNames) {
    Identification bestIdentification = null;
    int bestIndex = Integer.MAX_VALUE;

    for(Map.Entry<Identifier, Tuple2<Identification, MediaDescriptor>> entry : store.findDescriptorsAndIdentifications(streamId).entrySet()) {
      if(entry.getValue() != null && entry.getValue().a != null) {
        int index = dataSourceNames.indexOf(entry.getKey().getDataSource().getName());

        if(index >= 0 && index < bestIndex) {
          bestIndex = index;
          bestIdentification = entry.getValue().a;
        }
      }
    }

    return bestIdentification;
  }

  private static Serie createSerieDescriptor(MediaStream mediaStream) {
    return new Serie(
      new ProductionIdentifier(DataSource.instance(MediaType.of("SERIE"), "LOCAL"), "" + mediaStream.getStream().getId().asInt()),
      new Details(
        mediaStream.getAttributes().get(Attribute.TITLE),
        null,
        null,
        null,
        null
      ),
      null,
      Collections.emptyList(),
      Collections.emptyList(),
      Collections.emptyList(),
      State.ENDED,
      null,
      0,
      Collections.emptyList()  // TODO local serie also need local seasons and episodes or nothing will be displayed for them
    );
  }

  private static MediaDescriptor findDataSourcePreferredDescriptor(Collection<Tuple2<Identification, MediaDescriptor>> set, List<String> dataSourceNames) {
    MediaDescriptor bestDescriptor = null;
    int bestIndex = Integer.MAX_VALUE;

    for(Tuple2<Identification, MediaDescriptor> tuple : set) {
      if(tuple.b != null) {
        int index = dataSourceNames.indexOf(tuple.b.getIdentifier().getDataSource().getName());

        if(index >= 0 && index < bestIndex) {
          bestIndex = index;
          bestDescriptor = tuple.b;
        }
      }
    }

    return bestDescriptor;
  }
}
