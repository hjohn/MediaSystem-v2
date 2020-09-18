package hs.mediasystem.ui.api;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ui.api.domain.Work;

import java.util.List;
import java.util.function.Predicate;

public interface WorksClient {
  List<Work> findNewest(int maximum, Predicate<MediaType> filter);
  List<Work> findAllByType(MediaType type, String tag);
  List<Work> findRootsByTag(String tag);
  List<Work> findTop100();
}
