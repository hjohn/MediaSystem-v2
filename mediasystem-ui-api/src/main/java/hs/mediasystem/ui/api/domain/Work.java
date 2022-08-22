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
  public static final Comparator<Work> BY_LAST_WATCHED_DATE = Comparator.comparing(Work::getState, Comparator.comparing((State d) -> d.lastConsumptionTime().orElse(null), Comparator.nullsLast(Comparator.naturalOrder())));

  private final WorkId id;
  private final MediaType type;
  private final Optional<Parent> parent;
  private final Details details;
  private final List<MediaStream> streams;
  private final Optional<MediaStream> primaryStream;

  public Work(WorkId id, MediaType type, Parent parent, Details details, List<MediaStream> streams) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(details == null) {
      throw new IllegalArgumentException("details cannot be null");
    }
    if(streams == null) {
      throw new IllegalArgumentException("streams cannot be null");
    }

    this.id = id;
    this.type = type;
    this.parent = Optional.ofNullable(parent);
    this.details = details;
    this.streams = streams;

    /*
     * Derived information:
     */

    this.primaryStream = determineBestStream();
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

  public State getState() {
    return primaryStream.map(MediaStream::state).orElse(State.EMPTY);
  }

  /**
   * Returns the watched fraction, if a stream is available.  It is possible for a Work to be
   * consumed but not fully watched.  If the stream has no known duration (yet), it is assumed
   * to be unwatched and zero is returned.
   *
   * @return a watched fraction
   */
  public OptionalDouble getWatchedFraction() {
    return primaryStream
      .map(s -> s.duration().map(d -> s.state().resumePosition().toSeconds() / (double)d.toSeconds()).orElse(0.0))
      .map(OptionalDouble::of)
      .orElse(OptionalDouble.empty());
  }

  public boolean isWatched() {
    return getState().consumed();
  }

  public List<MediaStream> getStreams() {
    return streams;
  }

  public Optional<MediaStream> getPrimaryStream() {
    return primaryStream;
  }

  @Override
  public String toString() {
    return "Work[" + id + ": '" + details.getTitle() + "']";
  }

  private Optional<MediaStream> determineBestStream() {
    // This could take into account that two or more streams form a whole, and must also
    // deal with alternative streams (different cuts)

    MediaStream bestStream = null;

    for(MediaStream stream : streams) {
      if(bestStream == null || stream.state().lastConsumptionTime().orElse(Instant.MIN).isAfter(bestStream.state().lastConsumptionTime().orElse(Instant.MIN))) {
        bestStream = stream;
      }
    }

    return Optional.ofNullable(bestStream);
  }
}
