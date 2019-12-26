package hs.mediasystem.db.resource;

import hs.mediasystem.db.DatabaseStreamStore;
import hs.mediasystem.db.StreamState;
import hs.mediasystem.db.StreamStateProvider;
import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.db.extract.DefaultStreamMetaDataStore;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Resource;
import hs.mediasystem.ext.basicmediatypes.domain.stream.State;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamAttributes;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ResourceStore {
  @Inject private DatabaseStreamStore streamStore;
  @Inject private StreamStateService stateService;
  @Inject private StreamStateProvider streamStateProvider;
  @Inject private DefaultStreamMetaDataStore metaDataStore;
  @Inject private DescriptorStore descriptorStore;
  @Inject private SerieHelper serieHelper;

  private static final List<String> dataSourcePriorities = List.of("TMDB", "LOCAL");
  private static final Comparator<Map.Entry<Identifier, Identification>> DATA_SOURCE_PRIORITY =
      Comparator.comparing((Map.Entry<Identifier, Identification> e) -> dataSourcePriorities.indexOf(e.getKey().getDataSource().getName()));

  public synchronized Optional<Resource> find(StreamID streamId) {
    return streamStore.findStream(streamId).map(this::toResource);
  }

  public synchronized List<Resource> findLastWatched(int maximum, Instant after) {
    return streamStateProvider.map(stream -> stream
      .map(StreamState::getStreamID)
      .map(this::find)
      .flatMap(Optional::stream)
      .filter(r -> r.getState().getLastWatchedTime().isPresent())
      .filter(r -> after == null || r.getState().getLastWatchedTime().map(lwt -> lwt.isAfter(after)).orElse(false))
      .sorted(Comparator.comparing((Resource r) -> r.getState().getLastWatchedTime().orElseThrow()).reversed())
      .limit(maximum)
      .collect(Collectors.toList())
    );
  }

  public synchronized List<Resource> findNewest(int maximum) {
    return streamStore.findNewest(maximum).stream()
      .map(BasicStream::getId)
      .map(this::find)
      .flatMap(Optional::stream)
      .collect(Collectors.toList());
  }

  private Resource toResource(BasicStream bs) {
    StreamID streamId = bs.getId();
    StreamID parentId = streamStore.findParentId(streamId).orElse(null);
    StreamMetaData md = metaDataStore.find(streamId);

    Instant lastWatchedTime = stateService.getLastWatchedTime(streamId);
    boolean watched = stateService.isWatched(streamId);
    int totalDuration = stateService.getTotalDuration(streamId);
    Duration resumePosition = Duration.ofSeconds(stateService.getResumePosition(streamId));

    if(md == null && totalDuration != -1) {
      md = new StreamMetaData(streamId, Duration.ofSeconds(totalDuration), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    Optional<Tuple2<MediaDescriptor, Identification>> tuple = findBestDescriptor(streamId);

    return new Resource(
      streamId,
      parentId,
      new StreamAttributes(bs.getType(), bs.getUri(), streamStore.findCreationTime(streamId).orElseThrow(), bs.getAttributes()),
      new State(lastWatchedTime, watched, resumePosition),
      md,
      tuple.map(t -> t.b).orElse(null),
      tuple.map(t -> t.a).orElse(null)
    );
  }

  private Optional<Tuple2<MediaDescriptor, Identification>> findBestDescriptor(StreamID streamId) {
    StreamID parentId = streamStore.findParentId(streamId).orElse(null);

    if(parentId != null) {
      return findBestIdentification(parentId)
        .flatMap(e -> identifierToEpisodeIdentification(e, streamStore.findStream(streamId).get().getAttributes()));
    }

    return findBestIdentification(streamId)
      .flatMap(this::identifierToProductioneIdentification);
  }

  private Optional<Entry<Identifier, Identification>> findBestIdentification(StreamID streamId) {
    return streamStore.findIdentifications(streamId).entrySet().stream()
      .sorted(DATA_SOURCE_PRIORITY)
      .findFirst();
  }

  private Optional<Tuple2<MediaDescriptor, Identification>> identifierToProductioneIdentification(Entry<Identifier, Identification> e) {
    return descriptorStore.find(e.getKey())
      .map(ep -> Tuple.of(ep, e.getValue()));
  }

  private Optional<Tuple2<MediaDescriptor, Identification>> identifierToEpisodeIdentification(Entry<Identifier, Identification> e, Attributes childAttributes) {
    return descriptorStore.find(e.getKey())
      .filter(Serie.class::isInstance)
      .map(Serie.class::cast)
      .map(s -> serieHelper.findChildDescriptors(s, childAttributes))
      .orElse(List.of())
      .stream()
      .findFirst()  // If multiple episodes are found, a reduce could be done
      .map(ep -> Tuple.of(ep, e.getValue()));
  }
}
