package hs.mediasystem.ui.api;

import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Work;

import java.util.List;
import java.util.Optional;

public interface WorkClient {
  List<Work> findChildren(WorkId workId);
  Optional<Work> find(WorkId workId);
  List<Work> findRecommendations(WorkId workId);
  List<Contribution> findContributions(WorkId id);
  List<VideoLink> findVideoLinks(WorkId id);
  void reidentify(WorkId id);
}
