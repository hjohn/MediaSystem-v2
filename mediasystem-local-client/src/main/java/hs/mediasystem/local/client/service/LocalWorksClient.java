package hs.mediasystem.local.client.service;

import hs.mediasystem.db.services.WorksService;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
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
import hs.mediasystem.util.Throwables;

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
    return Throwables.uncheck(() -> worksService.findTop100()).stream().map(this::toWork).collect(Collectors.toList());
  }

  Work toWork(hs.mediasystem.ext.basicmediatypes.domain.stream.Work work) {
    List<MediaStream> streams = work.getStreams().stream().map(LocalWorksClient::toStream).collect(Collectors.toList());
    MediaStream primary = streams.isEmpty() ? null : streams.get(0);

    return new Work(
      work.getId(),
      work.getType(),
      createParent(work.getParent().orElse(null)),
      createDetails(work.getDescriptor(), work.getParent().orElse(null), primary),
      streams
    );
  }

  private Parent createParent(hs.mediasystem.domain.work.Parent parent) {
    return parent == null ? null : new Parent(
      parent.id(),
      parent.title(),
      parent.backdrop().map(imageHandleFactory::fromURI)
    );
  }

  /*
   * Note:
   * - auto cover image is created from multiple snapshots
   * - sample image is taken first snapshot if missing (don't use backdrop, it is not a sample!)
   * - backdrop is taken from parent or from the second snapshot if missing
   */
  private Details createDetails(WorkDescriptor descriptor, hs.mediasystem.domain.work.Parent parent, MediaStream stream) {
    Optional<MediaStream> mediaStream = Optional.ofNullable(stream);

    return new Details(
      descriptor.getDetails().getTitle(),
      descriptor.getDetails().getSubtitle().orElse(null),
      descriptor.getDetails().getDescription().orElse(null),
      descriptor.getDetails().getDate().orElse(null),
      descriptor.getDetails().getCover()
        .map(imageHandleFactory::fromURI)
        .orElse(null),
      mediaStream.map(LocalWorksClient::snapshotsToCover)
        .map(imageHandleFactory::fromURI)
        .orElse(null),
      descriptor.getDetails().getSampleImage()
        .or(() -> mediaStream.map(LocalWorksClient::snapshotsToSampleImage))
        .map(imageHandleFactory::fromURI)
        .orElse(null),
      descriptor.getDetails().getBackdrop()
        .or(() -> parent == null ? Optional.empty() : parent.backdrop())
        .or(() -> mediaStream.map(LocalWorksClient::snapshotsToBackdrop))
        .map(imageHandleFactory::fromURI)
        .orElse(null),
      descriptor instanceof Production m ? m.getTagLine().orElse(null) : null,
      descriptor instanceof Serie s ? createSerie(s) : null,
      parent != null && parent.getType() == MediaType.SERIE ? createSequence(descriptor) : null,
      descriptor instanceof Release r ? r.getReception() : null,
      descriptor instanceof Production p ? p.getPopularity() : null,
      descriptor instanceof Production p ? createClassification(p) : Classification.DEFAULT
    );
  }

  private static ImageURI snapshotsToCover(MediaStream mediaStream) {
    int id = mediaStream.contentId().asInt();

    return mediaStream.mediaStructure()
      .filter(ms -> !ms.videoTracks().isEmpty())
      .map(ms -> new ImageURI("multi:600,900;38,3,524,294;38,303,524,294;38,603,524,294:localdb://" + id + "/1|localdb://" + id + "/2|localdb://" + id + "/3", null))
      .orElse(null);
  }

  private static ImageURI snapshotsToSampleImage(MediaStream mediaStream) {
    int id = mediaStream.contentId().asInt();

    return mediaStream.mediaStructure()
      .filter(ms -> !ms.videoTracks().isEmpty())
      .map(ms -> new ImageURI("localdb://" + id + "/1", null))
      .orElse(null);
  }

  private static ImageURI snapshotsToBackdrop(MediaStream mediaStream) {
    int id = mediaStream.contentId().asInt();

    return mediaStream.mediaStructure()
      .filter(ms -> !ms.videoTracks().isEmpty())
      .map(ms -> new ImageURI("localdb://" + id + "/2", null))
      .orElse(null);
  }

  private static State toState(hs.mediasystem.domain.work.State state) {
    return new State(
      state.lastConsumptionTime(),
      state.consumed(),
      state.resumePosition()
    );
  }

  private static Classification createClassification(Production production) {
    hs.mediasystem.ext.basicmediatypes.domain.Classification c = production.getClassification();

    return new Classification(
      c.keywords().stream().map(Keyword::getText).collect(Collectors.toList()),
      c.genres(),
      c.languages(),
      c.contentRatings(),
      c.pornographic(),
      Optional.ofNullable(production instanceof Movie m ? toStage(m.getState()) : production instanceof Serie s ? toStage(s.getState()) : null)
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
      Optional.ofNullable(serie.getLastAirDate()),
      Optional.ofNullable(seasons == null ? null : (int)seasons.stream().filter(s -> s.getNumber() > 0).count()),
      Optional.ofNullable(seasons == null ? null : (int)seasons.stream().filter(s -> s.getNumber() > 0).map(Season::getEpisodes).flatMap(Collection::stream).count())
    );
  }

  private static Sequence createSequence(Episode episode) {
    return new Sequence(
      episode.getSeasonNumber() == 0 ? Type.SPECIAL : Type.EPISODE,
      episode.getNumber(),
      Optional.ofNullable(episode.getSeasonNumber() <= 0 ? null : episode.getSeasonNumber())
    );
  }

  private static Sequence createSequence(WorkDescriptor descriptor) {
    if(descriptor instanceof Episode episode) {
      return createSequence(episode);
    }

    return new Sequence(Type.EXTRA, 0, Optional.empty());
  }

  private static MediaStream toStream(hs.mediasystem.domain.work.MediaStream stream) {
    return new MediaStream(
      stream.location(),
      stream.id().getContentId(),
      stream.discoveryTime(),
      stream.lastModificationTime(),
      stream.size(),
      toState(stream.state()),
      stream.duration(),
      stream.mediaStructure(),
      stream.snapshots(),
      stream.match()
    );
  }
}
