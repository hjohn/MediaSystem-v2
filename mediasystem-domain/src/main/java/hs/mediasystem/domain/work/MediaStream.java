package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.util.Attributes;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Represents a stream.
 *
 * @param id a {@link StreamID}, never {@code null}
 * @param parentId an optional parent {@link StreamID}, never {@code null} but can be empty
 * @param uri a {@link URI}, never {@code null}
 * @param discoveryTime the time the item was first discovered, never {@code null}
 * @param lastModificationTime the time the item was last modified, never {@code null}
 * @param size the optional size of the item, never {@code null} or negative but can be empty
 * @param attributes the {@link Attributes} for this item, never {@code null}
 * @param state the {@link State} of this item, never {@code null}
 * @param duration the optional {@link Duration} of the item, never {@code null} but can be empty
 * @param mediaStructure the optional {@link MediaStructure} of the item, never {@code null} but can be empty
 * @param snapshots a list of {@link Snapshot}s for the item, never {@code null} but can be empty
 * @param match a {@link Match} for the item, never {@code null}
 */
public record MediaStream(StreamID id, Optional<StreamID> parentId, URI uri, Instant discoveryTime, Instant lastModificationTime, Optional<Long> size, Attributes attributes, State state, Optional<Duration> duration, Optional<MediaStructure> mediaStructure, List<Snapshot> snapshots, Match match) {
  public MediaStream {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(parentId == null) {
      throw new IllegalArgumentException("parentId cannot be null");
    }
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(discoveryTime == null) {
      throw new IllegalArgumentException("discoveryTime cannot be null");
    }
    if(lastModificationTime == null) {
      throw new IllegalArgumentException("lastModifiedTime cannot be null");
    }
    if(size == null) {
      throw new IllegalArgumentException("size cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }
    if(state == null) {
      throw new IllegalArgumentException("state cannot be null");
    }
    if(duration == null) {
      throw new IllegalArgumentException("duration cannot be null");
    }
    if(mediaStructure == null) {
      throw new IllegalArgumentException("mediaStructure cannot be null");
    }
    if(snapshots == null) {
      throw new IllegalArgumentException("snapshots cannot be null");
    }
    if(match == null) {
      throw new IllegalArgumentException("match cannot be null");
    }

    size.ifPresent(s -> {
      if(s < 0) {
        throw new IllegalArgumentException("size cannot be negative: " + s);
      }
    });
  }
}
