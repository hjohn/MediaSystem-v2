package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a work of art, like a movie, serie or episode, or a single stream of an unknown type.<p>
 *
 * The descriptor of a Work can either be supplied from an external database or if unavailable,
 * generated on demand with what little information is available.<p>
 *
 * In all cases, the streams which are part of a work all match with the descriptor, in other words
 * all streams that are part of a work will have the same work id.  When multiple streams are
 * part of a work they will be different cuts, encodings or even parts of the same work.  A stream
 * can also represent more than just the work it is associated with, eg. multiple episodes in a single
 * stream can belong to multiple works.
 */
public class Work {
  private final WorkDescriptor descriptor;
  private final List<MediaStream> streams;
  private final Optional<Parent> parent;

  public Work(WorkDescriptor descriptor, Parent parent, List<MediaStream> streams) {
    if(descriptor == null) {
      throw new IllegalArgumentException("descriptor cannot be null");
    }
    if(streams == null || streams.stream().filter(Objects::isNull).findAny().isPresent()) {
      throw new IllegalArgumentException("streams cannot be null or contain nulls: " + streams);
    }

    this.parent = Optional.ofNullable(parent);
    this.descriptor = descriptor;
    this.streams = streams;
  }

  public WorkId getId() {
    return descriptor.getId();
  }

  public Details getDetails() {
    return descriptor.getDetails();
  }

  public Optional<Parent> getParent() {
    return parent;
  }

  public MediaType getType() {
    return getId().getType();
  }

  public WorkDescriptor getDescriptor() {
    return descriptor;
  }

  public List<MediaStream> getStreams() {
    return streams;
  }

  public Optional<MediaStream> getPrimaryStream() {
    return streams.isEmpty() ? Optional.empty() : Optional.of(streams.get(0));
  }

  @Override
  public String toString() {
    return "Work[" + getType() + " " + descriptor + "]";
  }
}
