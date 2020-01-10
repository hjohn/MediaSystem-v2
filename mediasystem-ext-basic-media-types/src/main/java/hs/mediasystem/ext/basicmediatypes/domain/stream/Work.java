package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.State;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Work {
  private final WorkId id;
  private final Optional<WorkId> parentId;
  private final MediaType type;
  private final MediaDescriptor descriptor;
  private final State state;
  private final List<MediaStream> streams;
  private final Optional<Parent> parent;

  public Work(MediaDescriptor descriptor, Parent parent, State state, List<MediaStream> streams) {
    if(descriptor == null) {
      throw new IllegalArgumentException("descriptor cannot be null");
    }
    if(streams == null || streams.stream().filter(Objects::isNull).findAny().isPresent()) {
      throw new IllegalArgumentException("streams cannot be null or contain nulls: " + streams);
    }
    if(state == null) {
      throw new IllegalArgumentException("state cannot be null");
    }

    this.id = new WorkId(descriptor.getIdentifier().getDataSource(), descriptor.getIdentifier().getId());
    this.parent = Optional.ofNullable(parent);
    this.parentId = Optional.ofNullable(descriptor.getIdentifier().getRootIdentifier() == null ? null : new WorkId(descriptor.getIdentifier().getRootIdentifier().getDataSource(), descriptor.getIdentifier().getRootIdentifier().getId()));
    this.type = descriptor.getIdentifier().getDataSource().getType();
    this.descriptor = descriptor;
    this.state = state;
    this.streams = streams;
  }

  public WorkId getId() {
    return id;
  }

  public Details getDetails() {
    return descriptor.getDetails();
  }

  public Optional<Parent> getParent() {
    return parent;
  }

  public Optional<WorkId> getParentId() {
    return parentId;
  }

  public MediaType getType() {
    return type;
  }

  public MediaDescriptor getDescriptor() {
    return descriptor;
  }

  public State getState() {
    return state;
  }

  public List<MediaStream> getStreams() {
    return streams;
  }

  public Optional<MediaStream> getPrimaryStream() {
    return streams.isEmpty() ? Optional.empty() : Optional.of(streams.get(0));
  }

  @Override
  public String toString() {
    return "Work[" + type + " " + descriptor + "]";
  }
}
