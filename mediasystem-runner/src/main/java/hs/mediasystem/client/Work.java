package hs.mediasystem.client;

import hs.mediasystem.ext.basicmediatypes.domain.stream.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Parent;
import hs.mediasystem.ext.basicmediatypes.domain.stream.WorkId;
import hs.mediasystem.scanner.api.MediaType;

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

  public State getState() {
    return state;
  }

  public List<MediaStream> getStreams() {
    return streams;
  }

  public Optional<MediaStream> getPrimaryStream() {
    return streams.stream().findFirst();
  }
}
