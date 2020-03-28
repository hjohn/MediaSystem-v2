package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StreamableStore {

  /**
   * Returns a {@link Streamable} matching the given {@link ContentID}.  This will
   * find top level streams as well as child streams.
   *
   * @param contentId a {@link ContentID} to find, cannot be null
   * @return an {@link Optional} containing a {@link Streamable}, never null but can be empty
   */
  Optional<Streamable> findStream(ContentID contentId);

  /**
   * Finds the {@link Identification} for the given {@link ContentID}.  This only works
   * for top level streams as child streams donot have associated {@link Match}es.
   *
   * @param contentId a {@link ContentID} to find, cannot be null
   * @return a {@link Optional} containing a {@link Identification}, never null but can be empty
   */
  Optional<Identification> findIdentification(ContentID contentId);

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
   * Finds the parent {@link ContentID} given a child {@link ContentID}.
   *
   * @param contentId a {@link ContentID}, cannot be null
   * @return an {@link Optional} containing a {@link ContentID}, never null but can be empty
   */
  Optional<ContentID> findParentId(ContentID contentId);

  List<Streamable> findChildren(ContentID contentId);

  StreamSource findStreamSource(ContentID contentId);
  List<Streamable> findNewest(int maximum);
}
