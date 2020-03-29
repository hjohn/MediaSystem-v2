package hs.mediasystem.db.services;

import hs.mediasystem.db.base.DatabaseStreamStore;
import hs.mediasystem.db.base.StreamState;
import hs.mediasystem.db.base.StreamStateProvider;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.Top100QueryService;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorksService {
  @Inject private DatabaseStreamStore streamStore;
  @Inject private StreamStateProvider streamStateProvider;
  @Inject private WorkService workService;
  @Inject private List<Top100QueryService> top100QueryServices;

  public synchronized List<Work> findLastWatched(int maximum, Instant after) {
    return streamStateProvider.map(stream -> stream
      .map(StreamState::getContentID)
      .map(workService::findFirst)
      .flatMap(Optional::stream)
      .filter(r -> r.getState().getLastConsumptionTime().isPresent())
      .filter(r -> after == null || r.getState().getLastConsumptionTime().map(lwt -> lwt.isAfter(after)).orElse(false))
      .sorted(Comparator.comparing((Work r) -> r.getState().getLastConsumptionTime().orElseThrow()).reversed())
      .limit(maximum)
      .collect(Collectors.toList())
    );
  }

  public synchronized List<Work> findNewest(int maximum) {
    return streamStore.findNewest(maximum).stream()
      .map(Streamable::getId)
      .map(workService::find)
      .flatMap(Optional::stream)
      .collect(Collectors.toList());
  }

  public synchronized List<Work> findAllByType(MediaType type, String tag) {
    return streamStore.findStreams(type, tag).stream()
      .map(workService::toWork)
      .collect(Collectors.toList());
  }

  public synchronized List<Work> findTop100() {
    return top100QueryServices.get(0).query().stream()
      .map(p -> workService.toWork(p, null))
      .collect(Collectors.toList());
  }
}
