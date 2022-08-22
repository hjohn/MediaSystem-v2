package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;

import java.io.IOException;
import java.util.List;

public interface VideoLinksQueryService {
  List<VideoLink> query(WorkId id) throws IOException;
}
