package hs.mediasystem.local.client.service;

import hs.mediasystem.db.services.WorksService;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Keyword;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Release;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ui.api.WorksClient;
import hs.mediasystem.ui.api.domain.Classification;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Stage;
import hs.mediasystem.ui.api.domain.State;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageURI;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalWorksClient implements WorksClient {
  @Inject private WorksService worksService;
  @Inject private StateFactory stateFactory;

  @Override
  public List<Work> findLastWatched(int maximum, Instant after) {
    return worksService.findLastWatched(maximum, after).stream().map(this::toWork).collect(Collectors.toList());
  }

  @Override
  public List<Work> findNewest(int maximum) {
    return worksService.findNewest(maximum).stream().map(this::toWork).collect(Collectors.toList());
  }

  @Override
  public List<Work> findAllByType(MediaType type, String tag) {
    return worksService.findAllByType(type, tag).stream().map(this::toWork).collect(Collectors.toList());
  }

  @Override
  public List<Work> findTop100() {
    return worksService.findTop100().stream().map(this::toWork).collect(Collectors.toList());
  }

  Work toWork(hs.mediasystem.ext.basicmediatypes.domain.stream.Work work, MediaDescriptor parent) {
    return new Work(
      work.getId(),
      work.getType(),
      work.getParent().orElse(null),
      createDetails(work.getDescriptor(), parent, work.getPrimaryStream().map(MediaStream::getId)),
      toState(work.getPrimaryStream().map(MediaStream::getId).orElse(null)),
      work.getStreams()
    );
  }

  Work toWork(hs.mediasystem.ext.basicmediatypes.domain.stream.Work work) {
    return toWork(work, null);
  }

  /*
   * Note:
   * - image is taken from descriptor or otherwise from stream
   * - backdrop is taken from descriptor or otherwise from its parent
   */
  private static Details createDetails(MediaDescriptor descriptor, MediaDescriptor parent, Optional<ContentID> contentId) {
    return new Details(
      descriptor.getDetails().getName(),
      descriptor.getDetails().getDescription().orElse(null),
      descriptor.getDetails().getDate().orElse(null),
      descriptor.getDetails().getImage().or(() -> contentId.map(ContentID::asInt).map(id -> new ImageURI("localdb://" + id + "/2"))).orElse(null),
      descriptor.getDetails().getBackdrop().orElse(parent == null ? null : parent.getDetails().getBackdrop().orElse(null)),
      descriptor instanceof Movie ? ((Movie)descriptor).getTagLine() : null,
      descriptor instanceof Serie ? createSerie((Serie)descriptor) : null,
      descriptor instanceof Episode ? createSequence((Episode)descriptor) : null,
      descriptor instanceof Release ? ((Release)descriptor).getReception() : null,
      descriptor instanceof Production ? ((Production)descriptor).getPopularity() : null,
      descriptor instanceof Production ? createClassification((Production)descriptor) : Classification.DEFAULT
    );
  }

  private State toState(ContentID contentId) {
    return stateFactory.create(contentId);
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

  private static hs.mediasystem.ui.api.domain.Serie createSerie(Serie serie) {
    return new hs.mediasystem.ui.api.domain.Serie(
      serie.getLastAirDate(),
      (int)serie.getSeasons().stream().filter(s -> s.getNumber() > 0).count(),
      (int)serie.getSeasons().stream().filter(s -> s.getNumber() > 0).map(Season::getEpisodes).flatMap(Collection::stream).count()
    );
  }

  private static Sequence createSequence(Episode episode) {
    return new Sequence(
      episode.getSeasonNumber() == 0 ? Type.SPECIAL : episode.getSeasonNumber() < 0 ? Type.EXTRA : Type.EPISODE,
      episode.getNumber(),
      episode.getSeasonNumber() <= 0 ? null : episode.getSeasonNumber()
    );
  }
}
