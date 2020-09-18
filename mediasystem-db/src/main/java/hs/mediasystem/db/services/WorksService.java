package hs.mediasystem.db.services;

import hs.mediasystem.db.base.DatabaseStreamStore;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.Top100QueryService;
import hs.mediasystem.util.Throwables;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorksService {
  @Inject private DatabaseStreamStore streamStore;
  @Inject private WorkService workService;
  @Inject private List<Top100QueryService> top100QueryServices;

  public synchronized List<Work> findNewest(int maximum, Predicate<MediaType> filter) {
    return streamStore.findNewest(maximum, filter).stream()
      .map(workService::toWork)
      .collect(Collectors.toList());
  }

  public synchronized List<Work> findAllByType(MediaType type, String tag) {
    return streamStore.findStreams(type, tag).stream()
      .map(workService::toWork)
      .collect(Collectors.toList());
  }

  public synchronized List<Work> findRootsByTag(String tag) {
    return streamStore.findRootStreams(tag).stream()
      .map(workService::toWork)
      .collect(Collectors.toList());
  }

  public synchronized List<Work> findTop100() {
    return Throwables.uncheck(() -> top100QueryServices.get(0).query()).stream()
      .map(p -> workService.toWork(p, null))
      .collect(Collectors.toList());
  }
}
