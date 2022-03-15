package hs.mediasystem.ui.api.domain;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.NaturalLanguage;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public class Work {
  public static final Comparator<Work> BY_NAME = Comparator.comparing(Work::getDetails, Details.ALPHABETICAL);
  public static final Comparator<Work> BY_SUBTITLE = Comparator.comparing(Work::getDetails, Comparator.comparing(d -> d.getSubtitle().orElse(null), Comparator.nullsLast(NaturalLanguage.ALPHABETICAL)));
  public static final Comparator<Work> BY_RELEASE_DATE = Comparator.comparing(Work::getDetails, Details.RELEASE_DATE);
  public static final Comparator<Work> BY_REVERSE_RELEASE_DATE = Comparator.comparing(Work::getDetails, Details.RELEASE_DATE_REVERSED);
  public static final Comparator<Work> BY_LAST_WATCHED_DATE = Comparator.comparing(Work::getState, Comparator.comparing((State d) -> d.getLastConsumptionTime().orElse(null), Comparator.nullsLast(Comparator.naturalOrder())));

  private final WorkId id;
  private final MediaType type;
  private final Optional<Parent> parent;
  private final Details details;
  private final State state;
  private final List<MediaStream> streams;

  public Work(WorkId id, MediaType type, Parent parent, Details details, State state, List<MediaStream> streams) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(details == null) {
      throw new IllegalArgumentException("details cannot be null");
    }
    if(state == null) {
      throw new IllegalArgumentException("state cannot be null");
    }
    if(streams == null) {
      throw new IllegalArgumentException("streams cannot be null");
    }

    this.id = id;
    this.type = type;
    this.parent = Optional.ofNullable(parent);
    this.details = details;
    this.state = state;
    this.streams = streams;
  }

  public WorkId getId() {
    return id;
  }

  public MediaType getType() {
    return type;
  }

  public Optional<Parent> getParent() {
    return parent;
  }

  public Details getDetails() {
    return details;
  }

  // TODO state returned here is not the same type as the one in MediaStream, should be removed and an alternative solution devised for updating viewed state in views
  public State getState() {
    return state;
  }

  /**
   * Returns the watched fraction, if a stream is available.  It is possible for a Work to be
   * consumed but not fully watched.  If the stream has no known duration (yet), it is assumed
   * to be unwatched and zero is returned.
   *
   * @return a watched fraction
   */
  public OptionalDouble getWatchedFraction() {
    MediaStream bestStream = determineBestStream();

    if(bestStream == null) {
      return OptionalDouble.empty();
    }

    long resumePosition = bestStream.getState().getResumePosition().toSeconds();

    return OptionalDouble.of(bestStream.getDuration().map(d -> resumePosition / (double)d.toSeconds()).orElse(0.0));
  }

  public boolean isWatched() {
    MediaStream bestStream = determineBestStream();

    return bestStream == null ? false : bestStream.getState().isConsumed();
  }

  private MediaStream determineBestStream() {
    // This could take into account that two or more streams form a whole, and must also
    // deal with alternative streams (different cuts)

    MediaStream bestStream = null;

    for(MediaStream stream : streams) {
      if(bestStream == null || stream.getState().getLastConsumptionTime().orElse(Instant.MIN).isAfter(bestStream.getState().getLastConsumptionTime().orElse(Instant.MIN))) {
        bestStream = stream;
      }
    }

    return bestStream;
  }

  public List<MediaStream> getStreams() {
    return streams;
  }

  public Optional<MediaStream> getPrimaryStream() {
    return streams.stream().findFirst();
  }

  @Override
  public String toString() {
    return "Work[" + id + ": '" + details.getTitle() + "']";
  }
}
