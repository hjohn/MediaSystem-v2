package hs.mediasystem.ui.api.domain;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.WorkId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class Work {
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

  public double getWatchedFraction() {
    MediaStream bestStream = determineBestStream();

    if(bestStream == null) {
      return 0;
    }

    long resumePosition = bestStream.getState().getResumePosition().toSeconds();

    return bestStream.getDuration().map(d -> resumePosition / (double)d.toSeconds()).orElse(0.0);
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
