package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

import java.util.List;
import java.util.Optional;

public interface StreamableStore {

  /**
   * Returns a {@link Streamable} matching the given {@link ContentID}.  This will
   * find top level streams as well as child streams.
   *
   * @param streamId a {@link StreamID} to find, cannot be null
   * @return an {@link Optional} containing a {@link Streamable}, never null but can be empty
   */
  Optional<Streamable> findStream(StreamID streamId);

  List<Streamable> findChildren(StreamID streamId);
}
