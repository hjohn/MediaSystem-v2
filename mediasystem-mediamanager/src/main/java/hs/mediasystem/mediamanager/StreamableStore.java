package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StreamableStore {

  /**
   * Returns a {@link Streamable} matching the given {@link StreamID}.  This will
   * find top level streams as well as child streams.
   *
   * @param streamId a {@link StreamID} to find, cannot be null
   * @return an {@link Optional} containing a {@link Streamable}, never null but can be empty
   */
  Optional<Streamable> findStream(StreamID streamId);

  /**
   * Finds the {@link Identification} for the given {@link StreamID}.  This only works
   * for top level streams as child streams donot have associated {@link Match}es.
   *
   * @param streamId a {@link StreamID} to find, cannot be null
   * @return a {@link Optional} containing a {@link Identification}, never null but can be empty
   */
  Optional<Identification> findIdentification(StreamID streamId);

  /**
   * Finds all {@link Streamable}s matching the given {@link Identifier}.
   *
   * @param identifier an {@link Identifier}, cannot be null
   * @return a {@link Set} containing all found {@link Streamable}s, never null but can be empty
   */
  Set<Streamable> findStreams(Identifier identifier);

  /**
   * Finds all {@link Streamable}s matching the given {@link MediaType} and tag.
   * This will only find top level streams as child streams donot have associated
   * tags.
   *
   * @param type a {@link MediaType}, cannot be null
   * @param tag a {@link String}, can be null
   * @return a {@link Set} containing all found {@link Streamable}s, never null but can be empty
   */
  Set<Streamable> findStreams(MediaType type, String tag);

  /**
   * Finds the parent {@link StreamID} given a child {@link StreamID}.
   *
   * @param streamId a {@link StreamID}, cannot be null
   * @return an {@link Optional} containing a {@link StreamID}, never null but can be empty
   */
  Optional<StreamID> findParentId(StreamID streamId);

  List<Streamable> findChildren(StreamID streamId);

  StreamSource findStreamSource(StreamID streamId);
  List<Streamable> findNewest(int maximum);
}
