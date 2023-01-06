package hs.mediasystem.db.services;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.Top100QueryService;
import hs.mediasystem.util.checked.CheckedStreams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorksService {
  @Inject private LinkedWorksService linkedWorksService;
  @Inject private WorkService workService;
  @Inject private LocalWorkService localWorkService;
  @Inject private List<Top100QueryService> top100QueryServices;

  public List<Work> findNewest(int maximum, Predicate<MediaType> filter) {
    return linkedWorksService.findNewest(maximum, filter).stream()
      .map(localWorkService::toWork)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  public List<Work> findAllByType(MediaType type, String tag) {
    return linkedWorksService.findAllByType(type, tag).stream()
      .map(localWorkService::toWork)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  public List<Work> findRootsByTag(String tag) {
    return linkedWorksService.findRootsByTag(tag).stream()
      .map(localWorkService::toWork)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  public List<Work> findTop100() throws IOException {
    return CheckedStreams.forIOException(top100QueryServices.get(0).query())
      .map(workService::toWork)
      .collect(Collectors.toCollection(ArrayList::new));
  }
}
