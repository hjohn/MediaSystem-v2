package hs.mediasystem.local.client.service;

import hs.mediasystem.db.services.WorksService;
import hs.mediasystem.domain.stream.MediaType;
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
import hs.mediasystem.ui.api.domain.MediaStream;
import hs.mediasystem.ui.api.domain.Parent;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Stage;
import hs.mediasystem.ui.api.domain.State;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.ImageURI;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalWorksClient implements WorksClient {
  @Inject private WorksService worksService;
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  public List<Work> findNewest(int maximum, Predicate<MediaType> filter) {
    return worksService.findNewest(maximum, filter).stream().map(this::toWork).collect(Collectors.toList());
  }

  @Override
  public List<Work> findAllByType(MediaType type, String tag) {
    return worksService.findAllByType(type, tag).stream().map(this::toWork).collect(Collectors.toList());
  }

  @Override
  public List<Work> findRootsByTag(String tag) {
    return worksService.findRootsByTag(tag).stream().map(this::toWork).collect(Collectors.toList());
  }

  @Override
  public List<Work> findTop100() {
    return worksService.findTop100().stream().map(this::toWork).collect(Collectors.toList());
  }

  Work toWork(hs.mediasystem.ext.basicmediatypes.domain.stream.Work work) {
    List<MediaStream> streams = work.getStreams().stream().map(LocalWorksClient::toStream).collect(Collectors.toList());
    MediaStream primary = streams.isEmpty() ? null : streams.get(0);

    return new Work(
      work.getId(),
      work.getType(),
      createParent(work.getParent().orElse(null)),
      createDetails(work.getDescriptor(), work.getParent().orElse(null), primary),
      primary == null ? State.EMPTY : primary.getState(),
      streams
    );
  }

  private Parent createParent(hs.mediasystem.domain.work.Parent parent) {
    return parent == null ? null : new Parent(
      parent.getId(),
      parent.getName(),
      parent.getBackdrop().map(imageHandleFactory::fromURI).orElse(null)
    );
  }

  /*
   * Note:
   * - cover image is created from multiple snapshots if missing
   * - sample image is taken first snapshot if missing (don't use backdrop, it is not a sample!)
   * - backdrop is taken from parent or from the second snapshot if missing
   */
  private Details createDetails(MediaDescriptor descriptor, hs.mediasystem.domain.work.Parent parent, MediaStream stream) {
    Optional<MediaStream> mediaStream = Optional.ofNullable(stream);

    return new Details(
      descriptor.getDetails().getTitle(),
      descriptor.getDetails().getSubtitle().orElse(null),
      descriptor.getDetails().getDescription().orElse(null),
      descriptor.getDetails().getDate().orElse(null),
      descriptor.getDetails().getCover()
        .or(() -> mediaStream.map(LocalWorksClient::snapshotsToCover))
        .map(imageHandleFactory::fromURI)
        .orElse(null),
      descriptor.getDetails().getSampleImage()
        .or(() -> mediaStream.map(LocalWorksClient::snapshotsToSampleImage))
        .map(imageHandleFactory::fromURI)
        .orElse(null),
      descriptor.getDetails().getBackdrop()
        .or(() -> parent == null ? Optional.empty() : parent.getBackdrop())
        .or(() -> mediaStream.map(LocalWorksClient::snapshotsToBackdrop))
        .map(imageHandleFactory::fromURI)
        .orElse(null),
      descriptor instanceof Movie ? ((Movie)descriptor).getTagLine() : null,
      descriptor instanceof Serie ? createSerie((Serie)descriptor) : null,
      descriptor instanceof Episode ? createSequence((Episode)descriptor) : null,
      descriptor instanceof Release ? ((Release)descriptor).getReception() : null,
      descriptor instanceof Production ? ((Production)descriptor).getPopularity() : null,
      descriptor instanceof Production ? createClassification((Production)descriptor) : Classification.DEFAULT
    );
  }

  private static ImageURI snapshotsToCover(MediaStream mediaStream) {
    int id = mediaStream.getId().getContentId().asInt();

    return mediaStream.getMediaStructure()
      .filter(ms -> !ms.getVideoTracks().isEmpty())
      .map(ms -> new ImageURI("multi:600,900;38,3,524,294;38,303,524,294;38,603,524,294:localdb://" + id + "/1|localdb://" + id + "/2|localdb://" + id + "/3", null))
      .orElse(null);
  }

  private static ImageURI snapshotsToSampleImage(MediaStream mediaStream) {
    int id = mediaStream.getId().getContentId().asInt();

    return mediaStream.getMediaStructure()
      .filter(ms -> !ms.getVideoTracks().isEmpty())
      .map(ms -> new ImageURI("localdb://" + id + "/1", null))
      .orElse(null);
  }

  private static ImageURI snapshotsToBackdrop(MediaStream mediaStream) {
    int id = mediaStream.getId().getContentId().asInt();

    return mediaStream.getMediaStructure()
      .filter(ms -> !ms.getVideoTracks().isEmpty())
      .map(ms -> new ImageURI("localdb://" + id + "/2", null))
      .orElse(null);
  }

  private static State toState(hs.mediasystem.domain.work.State state) {
    return new State(
      state.getLastConsumptionTime().orElse(null),
      state.isConsumed(),
      state.getResumePosition()
    );
  }

  private static Classification createClassification(Production production) {
    hs.mediasystem.ext.basicmediatypes.domain.Classification c = production.getClassification();

    return new Classification(
      c.getKeywords().stream().map(Keyword::getText).collect(Collectors.toList()),
      c.getGenres(),
      c.getLanguages(),
      c.getContentRatings(),
      c.getPornographic(),
      production instanceof Movie ? toStage(((Movie)production).getState()) : production instanceof Serie ? toStage(((Serie)production).getState()) : null
    );
  }

  private static Stage toStage(Serie.State state) {
    return state == Serie.State.CANCELED || state == Serie.State.ENDED ? Stage.ENDED
      : state == Serie.State.CONTINUING || state == Serie.State.PILOT ? Stage.RELEASED
      : state == Serie.State.IN_PRODUCTION ? Stage.IN_PRODUCTION
      : state == Serie.State.PLANNED ? Stage.PLANNED
      : null;
  }

  private static Stage toStage(Movie.State state) {
    return state == Movie.State.IN_PRODUCTION ? Stage.IN_PRODUCTION :
      state == Movie.State.PLANNED ? Stage.PLANNED : Stage.RELEASED;
  }

  private static hs.mediasystem.ui.api.domain.Serie createSerie(Serie serie) {
    List<Season> seasons = serie.getSeasons();

    return new hs.mediasystem.ui.api.domain.Serie(
      serie.getLastAirDate(),
      seasons == null ? null : (int)seasons.stream().filter(s -> s.getNumber() > 0).count(),
      seasons == null ? null : (int)seasons.stream().filter(s -> s.getNumber() > 0).map(Season::getEpisodes).flatMap(Collection::stream).count()
    );
  }

  private static Sequence createSequence(Episode episode) {
    return new Sequence(
      episode.getSeasonNumber() == 0 ? Type.SPECIAL : episode.getSeasonNumber() < 0 ? Type.EXTRA : Type.EPISODE,
      episode.getNumber(),
      episode.getSeasonNumber() <= 0 ? null : episode.getSeasonNumber()
    );
  }

  private static MediaStream toStream(hs.mediasystem.domain.work.MediaStream stream) {
    return new MediaStream(
      stream.getId(),
      stream.getParentId().orElse(null),
      stream.getAttributes(),
      toState(stream.getState()),
      stream.getDuration().orElse(null),
      stream.getMediaStructure().orElse(null),
      stream.getSnapshots(),
      stream.getMatch().orElse(null)
    );
  }
}
