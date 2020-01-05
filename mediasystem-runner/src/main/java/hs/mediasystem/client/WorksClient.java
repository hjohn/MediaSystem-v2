package hs.mediasystem.client;

import hs.mediasystem.client.Sequence.Type;
import hs.mediasystem.db.services.WorksService;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Keyword;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Release;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.MediaStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorksClient {
  @Inject private WorksService worksService;
  @Inject private State.Factory stateFactory;

  public List<Work> findLastWatched(int maximum, Instant after) {
    return worksService.findLastWatched(maximum, after).stream().map(this::toWork).collect(Collectors.toList());
  }

  public List<Work> findNewest(int maximum) {
    return worksService.findNewest(maximum).stream().map(this::toWork).collect(Collectors.toList());
  }

  public List<Work> findAllByType(MediaType type, String tag) {
    return worksService.findAllByType(type, tag).stream().map(this::toWork).collect(Collectors.toList());
  }

  public List<Work> findTop100() {
    return worksService.findTop100().stream().map(this::toWork).collect(Collectors.toList());
  }

  Work toWork(hs.mediasystem.ext.basicmediatypes.domain.stream.Work work) {
    return new Work(
      work.getId(),
      work.getType(),
      work.getParent().orElse(null),
      createDetails(work.getDescriptor()),
      toState(work.getPrimaryStream().map(MediaStream::getId).orElse(null)),
      work.getStreams()
    );
  }

  private static Details createDetails(MediaDescriptor descriptor) {
    return new Details(
      descriptor.getDetails().getName(),
      descriptor.getDetails().getDescription().orElse(null),
      descriptor.getDetails().getDate().orElse(null),
      descriptor instanceof Serie ? ((Serie)descriptor).getLastAirDate() : null,
      descriptor.getDetails().getImage().orElse(null),
      descriptor.getDetails().getBackdrop().orElse(null),
      descriptor instanceof Movie ? ((Movie)descriptor).getTagLine() : null,
      descriptor instanceof Episode ? createSequence((Episode)descriptor) : null,
      descriptor instanceof Release ? ((Release)descriptor).getReception() : null,
      descriptor instanceof Production ? ((Production)descriptor).getPopularity() : null,
      descriptor instanceof Production ? createClassification((Production)descriptor) : Classification.DEFAULT
    );
  }

  private State toState(StreamID streamId) {
    return stateFactory.create(streamId);
  }

  private static Classification createClassification(Production production) {
    return new Classification(
      production instanceof Movie ? ((Movie)production).getKeywords().stream().map(Keyword::getText).collect(Collectors.toList()) : List.<String>of(),
      production.getGenres(),
      production instanceof Movie ? toStage(((Movie)production).getState()) : production instanceof Serie ? toStage(((Serie)production).getState()) : null
    );
  }

  private static Stage toStage(Serie.State state) {
    return state == Serie.State.CANCELED || state == Serie.State.ENDED ? Stage.ENDED
      : state == Serie.State.CONTINUING || state == Serie.State.PILOT ? Stage.RELEASED
      : state == Serie.State.IN_PRODUCTION ? Stage.IN_PRODUCTION
      : state == Serie.State.PLANNED ? Stage.PLANNED
      : Stage.RELEASED;
  }

  private static Stage toStage(Movie.State state) {
    return state == Movie.State.IN_PRODUCTION ? Stage.IN_PRODUCTION :
      state == Movie.State.PLANNED ? Stage.PLANNED : Stage.RELEASED;
  }

  private static Sequence createSequence(Episode episode) {
    return new Sequence(
      episode.getSeasonNumber() == 0 ? Type.SPECIAL : episode.getSeasonNumber() < 0 ? Type.EXTRA : Type.EPISODE,
      episode.getNumber(),
      episode.getSeasonNumber() <= 0 ? null : episode.getSeasonNumber()
    );
  }
}
