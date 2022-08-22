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
import hs.mediasystem.domain.work.MediaStructure;
import hs.mediasystem.domain.work.State;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.mediamanager.StreamMetaDataStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaStreamService {
  @Inject private StreamStateService stateService;
  @Inject private StreamMetaDataStore metaDataStore;

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
    StreamMetaData md = metaDataStore.find(id.getContentId()).orElse(null);
    int totalDuration = stateService.getTotalDuration(id.getContentId());

    if(md == null && totalDuration != -1) {
      md = new StreamMetaData(id.getContentId(), Duration.ofSeconds(totalDuration), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    return new MediaStream(
      id,
      resource.parentId().orElse(null),
      resource.uri(),
      resource.discoveryTime(),
      resource.lastModificationTime(),
      resource.size().orElse(null),
      resource.attributes(),
      state,
      md == null ? (totalDuration != -1 ? Duration.ofSeconds(totalDuration) : null) : md.getLength().orElse(null),
      md == null ? null : new MediaStructure(md.getVideoTracks(), md.getAudioTracks(), md.getSubtitleTracks()),
      md == null ? List.of() : md.getSnapshots(),
      match
    );
  }

  private State toState(ContentID contentId) {
    // TODO for Series, need to compute last watched time and watched status based on its children

    Instant lastWatchedTime = stateService.getLastWatchedTime(contentId);
    boolean watched = stateService.isWatched(contentId);
    Duration resumePosition = Duration.ofSeconds(stateService.getResumePosition(contentId));

    return new State(lastWatchedTime, watched, resumePosition);
  }
}
