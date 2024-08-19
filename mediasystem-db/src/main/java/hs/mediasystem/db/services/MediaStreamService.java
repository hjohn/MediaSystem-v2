package hs.mediasystem.db.services;

import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.media.MediaStream;
import hs.mediasystem.domain.media.MediaStructure;
import hs.mediasystem.domain.media.StreamDescriptor;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.State;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaStreamService {
  @Inject private StreamStateService stateService;

  public MediaStream toMediaStream(LinkedWork linkedWork) {
    return linkedWork.resources().stream().findFirst().map(this::toMediaStream).orElseThrow();
  }

  public MediaStream toMediaStream(Resource resource) {
    State state = toState(resource.contentId());
    int totalDuration = stateService.getTotalDuration(resource.contentId());

    return new MediaStream(
      resource.location(),
      resource.contentId(),
      resource.streamable().contentPrint().getSignatureCreationTime(),
      resource.streamable().contentPrint().getLastModificationTime(),
      Optional.ofNullable(resource.streamable().contentPrint().getSize()),
      state,
      resource.streamable().descriptor().flatMap(StreamDescriptor::duration).or(() -> Optional.ofNullable(totalDuration != -1 ? Duration.ofSeconds(totalDuration) : null)),
      resource.streamable().descriptor().map(sd -> new MediaStructure(sd.videoTracks(), sd.audioTracks(), sd.subtitleTracks())),
      resource.streamable().descriptor().map(StreamDescriptor::snapshots).orElse(List.of()),
      resource.match()
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
