package hs.mediasystem.ui.api.domain;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.MediaStructure;
import hs.mediasystem.domain.work.Snapshot;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Represents a stream.
 *
 * @param location a {@link URI}, never {@code null}
 * @param contentId a {@link ContentID}, never {@code null}
 * @param discoveryTime the time the item was first discovered, never {@code null}
 * @param lastModificationTime the time the item was last modified, never {@code null}
 * @param size the optional size of the item, never {@code null} or negative but can be empty
 * @param state the {@link State} of this item, never {@code null}
 * @param duration the optional {@link Duration} of the item, never {@code null} but can be empty
 * @param mediaStructure the optional {@link MediaStructure} of the item, never {@code null} but can be empty
 * @param snapshots a list of {@link Snapshot}s for the item, never {@code null} but can be empty
 * @param match a {@link Match} for the item, never {@code null}
 */
public record MediaStream(URI location, ContentID contentId, Instant discoveryTime, Instant lastModificationTime, Optional<Long> size, State state, Optional<Duration> duration, Optional<MediaStructure> mediaStructure, List<Snapshot> snapshots, Match match) {
  public MediaStream {
    if(location == null) {
      throw new IllegalArgumentException("location cannot be null");
    }
    if(contentId == null) {
      throw new IllegalArgumentException("contentId cannot be null");
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
