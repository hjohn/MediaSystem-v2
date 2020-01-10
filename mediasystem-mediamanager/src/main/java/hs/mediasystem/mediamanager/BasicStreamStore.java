package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.BasicStream;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface BasicStreamStore {

  /**
   * Returns a {@link BasicStream} matching the given {@link StreamID}.  This will
   * find top level streams as well as child streams.
   *
   * @param streamId a {@link StreamID} to find, cannot be null
   * @return an {@link Optional} containing a {@link BasicStream}, never null but can be empty
   */
  Optional<BasicStream> findStream(StreamID streamId);

  /**
   * Finds {@link Identifier}s and {@link Identification}s for the given
   * {@link StreamID}.  This only works for top level streams as child streams
   * donot have associated {@link Identification}s.
   *
   * @param streamId a {@link StreamID} to find, cannot be null
   * @return a {@link Map} containing all found {@link Identifier}s, never null but can be empty
   */
  Map<Identifier, Identification> findIdentifications(StreamID streamId);

  /**
   * Finds all {@link BasicStream}s matching the given {@link Identifier}.  This
   * will only find top level streams as child streams donot have associated
   * {@link Identification}s.
   *
   * @param identifier an {@link Identifier}, cannot be null
   * @return a {@link Set} containing all found {@link BasicStream}s, never null but can be empty
   */
  Set<BasicStream> findStreams(Identifier identifier);

  /**
   * Finds all {@link BasicStream}s matching the given {@link MediaType} and tag.
   * This will only find top level streams as child streams donot have associated
   * tags.
   *
   * @param type a {@link MediaType}, cannot be null
   * @param tag a {@link String}, can be null
   * @return a {@link Set} containing all found {@link BasicStream}s, never null but can be empty
   */
  Set<BasicStream> findStreams(MediaType type, String tag);

  /**
   * Finds the parent {@link StreamID} given a child {@link StreamID}.
   *
   * @param streamId a {@link StreamID}, cannot be null
   * @return an {@link Optional} containing a {@link StreamID}, never null but can be empty
   */
  Optional<StreamID> findParentId(StreamID streamId);

  StreamSource findStreamSource(StreamID streamId);
  Map<BasicStream, Map<Identifier, Identification>> findIdentifiersByStreams(MediaType type, String tag);
  List<BasicStream> findNewest(int maximum);
}
