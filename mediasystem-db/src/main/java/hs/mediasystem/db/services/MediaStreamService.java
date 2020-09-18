package hs.mediasystem.db.services;

import hs.mediasystem.db.base.DatabaseStreamStore;
import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.extract.DefaultStreamMetaDataStore;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.MediaStructure;
import hs.mediasystem.domain.work.State;
import hs.mediasystem.domain.work.StreamAttributes;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaStreamService {
  @Inject private DatabaseStreamStore streamStore;
  @Inject private StreamStateService stateService;
  @Inject private DefaultStreamMetaDataStore metaDataStore;
  @Inject private ContentPrintProvider contentPrintProvider;

  public synchronized Optional<MediaStream> findFirst(ContentID contentId) {
    return streamStore.findStreams(contentId).stream().findFirst().map(this::toMediaStream);
  }

  public MediaStream toMediaStream(Streamable streamable) {
    Match match = streamStore.findIdentification(streamable.getId()).map(Identification::getMatch).orElse(null);
    StreamID id = streamable.getId();
    StreamID parentId = streamStore.findParentId(id).orElse(null);
    State state = toState(streamable);
    StreamMetaData md = metaDataStore.find(id.getContentId()).orElse(null);
    int totalDuration = stateService.getTotalDuration(id.getContentId());

    if(md == null && totalDuration != -1) {
      md = new StreamMetaData(id.getContentId(), Duration.ofSeconds(totalDuration), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    ContentPrint contentPrint = contentPrintProvider.get(id.getContentId());

    return new MediaStream(
      id,
      parentId,
      new StreamAttributes(streamable.getUri(), streamStore.findCreationTime(id).orElseThrow(), Instant.ofEpochMilli(contentPrint.getLastModificationTime()), contentPrint.getSize(), streamable.getAttributes()),
      state,
      md != null ? md.getLength() : totalDuration != -1 ? Duration.ofSeconds(totalDuration) : null,
      md == null ? null : new MediaStructure(md.getVideoTracks(), md.getAudioTracks(), md.getSubtitleTracks()),
      md == null ? List.of() : md.getSnapshots(),
      match
    );
  }

  private State toState(Streamable streamable) {
    ContentID contentId = streamable.getId().getContentId();

    // TODO for Series, need to compute last watched time and watched status based on its children

    Instant lastWatchedTime = stateService.getLastWatchedTime(contentId);
    boolean watched = stateService.isWatched(contentId);
    Duration resumePosition = Duration.ofSeconds(stateService.getResumePosition(contentId));

    return new State(lastWatchedTime, watched, resumePosition);
  }
}
