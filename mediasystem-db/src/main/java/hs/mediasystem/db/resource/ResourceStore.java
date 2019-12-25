package hs.mediasystem.db.resource;

import hs.mediasystem.db.DatabaseStreamStore;
import hs.mediasystem.db.StreamState;
import hs.mediasystem.db.StreamStateProvider;
import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.db.extract.DefaultStreamMetaDataStore;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Resource;
import hs.mediasystem.ext.basicmediatypes.domain.stream.State;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamAttributes;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.StreamID;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

    return new Resource(
      streamId,
      parentId,
      new StreamAttributes(bs.getType(), bs.getUri(), streamStore.findCreationTime(streamId).orElseThrow(), bs.getAttributes()),
      new State(lastWatchedTime, watched, resumePosition),
      md
    );
  }
}
