package hs.mediasystem.db.services;

import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.services.domain.LinkedResource;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.MatchedResource;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.State;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaStreamService {
  @Inject private StreamStateService stateService;

  public MediaStream toMediaStream(LinkedResource linkedResource) {
    return toMediaStream(linkedResource.match(), linkedResource.resource());
  }

  public MediaStream toMediaStream(MatchedResource matchedResource) {
    return toMediaStream(matchedResource.match(), matchedResource.resource());
  }

  public MediaStream toMediaStream(LinkedWork linkedWork) {
    return linkedWork.matchedResources().stream().findFirst().map(this::toMediaStream).orElseThrow();
  }

  private MediaStream toMediaStream(Match match, Resource resource) {
    StreamID id = resource.id();
    State state = toState(id.getContentId());
    int totalDuration = stateService.getTotalDuration(id.getContentId());

    return new MediaStream(
      id,
      resource.parentId(),
      resource.uri(),
      resource.discoveryTime(),
      resource.lastModificationTime(),
      resource.size(),
      resource.attributes(),
      state,
      resource.duration().or(() -> Optional.ofNullable(totalDuration != -1 ? Duration.ofSeconds(totalDuration) : null)),
      resource.mediaStructure(),
      resource.snapshots(),
      match
    );
  }

  private State toState(ContentID contentId) {
    // TODO for Series, need to compute last watched time and watched status based on its children

    Instant lastWatchedTime = stateService.getLastWatchedTime(contentId);
    boolean watched = stateService.isWatched(contentId);
    Duration resumePosition = Duration.ofSeconds(stateService.getResumePosition(contentId));

    return new State(Optional.ofNullable(lastWatchedTime), watched, resumePosition);
  }
}
