package hs.mediasystem.ui.api;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ui.api.domain.Work;

import java.time.Instant;
import java.util.List;

public interface WorksClient {
  List<Work> findLastWatched(int maximum, Instant after);
  List<Work> findNewest(int maximum);
  List<Work> findAllByType(MediaType type, String tag);
  List<Work> findRootsByTag(String tag);
  List<Work> findTop100();
}
