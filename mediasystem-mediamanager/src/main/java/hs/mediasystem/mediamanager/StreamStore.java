package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;
import hs.mediasystem.util.Tuple.Tuple3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Keeps track of {@link BasicStream}s and {@link StreamID}s and their optionaly associated {@link Identifier}s,
 * {@link Identification}s and {@link MediaDescriptor}s.<p>
 *
 * The primary function of this class is to provide fast calls to find information based on {@link StreamID} and
 * {@link Identifier}.  Several indices are created and updated for this purpose.<p>
 *
 * The relationship between StreamID's and Identifiers is many-to-many, so a single StreamID can refer to multiple
 * Identifiers and vice versa. For example, a movie may be identified by multiple different services, and conversely
 * a specific movie may have multiple copies available.<p>
 *
 * Streams only optionally have one (or more) Identifier plus Identification, and in turn, an Identifier can
 * optionally have a Descriptor.  In other words, even unidentified a stream can exist, and if identified it may
 * not have any actual information associated with it (yet).
 */
@Singleton
public class StreamStore {
  @Inject private EpisodeMatcher episodeMatcher;  // TODO wierd dependency

  private final Map<StringURI, StreamID> byUri = new HashMap<>();
  private final Map<StreamID, BasicStream> streams = new HashMap<>();
  private final Map<StreamID, StreamSource> streamSources = new HashMap<>();

  private final BiMultiMap<StreamID, Identifier, Tuple2<Identification, MediaDescriptor>> bmm = new BiMultiMap<>();

  synchronized void put(BasicStream stream, StreamSource source, Set<Tuple3<Identifier, Identification, MediaDescriptor>> records) {
    remove(stream);

    add(stream, source, null, null, null);

    for(Tuple3<Identifier, Identification, MediaDescriptor> tuple : records) {
      add(stream, source, tuple.a, tuple.b, tuple.c);
    }
  }

  synchronized void update(BasicStream stream, Set<Tuple3<Identifier, Identification, MediaDescriptor>> records) {
    put(stream, streamSources.get(stream.getId()), records);
  }

  private synchronized void add(BasicStream stream, StreamSource source, Identifier identifier, Identification identification, MediaDescriptor descriptor) {
    if(stream == null) {
      throw new IllegalArgumentException("stream cannot be null");
    }
    if(identifier != null && identification == null) {
      throw new IllegalArgumentException("identification cannot be null when identifier is not null");
    }
    if(identification != null && identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null when identification is not null");
    }
    if(descriptor != null && identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null when descriptor is not null");
    }
    if(descriptor != null && identifier != null && !descriptor.getIdentifier().equals(identifier)) {
      throw new IllegalArgumentException("identifier from descriptor must match identifier");
    }

    byUri.put(stream.getUri(), stream.getId());
    streams.put(stream.getId(), stream);

    if(source != null) {
      streamSources.put(stream.getId(), source);
    }

    if(identifier != null) {
      bmm.associate(
        stream.getId(),
        identifier,
        Tuple.of(identification, descriptor)
      );

      // Handle child streams if there is a descriptor for the parent (otherwise pointless since parent could be anything):
      if(descriptor != null) {
        nextChild:
        for(BasicStream childStream : stream.getChildren()) {
          Attributes attributes = childStream.getAttributes();

          if(!"EXTRA".equals(attributes.get(Attribute.CHILD_TYPE))) {
            Tuple2<Identification, List<Episode>> match = episodeMatcher.attemptMatch((Serie)descriptor, identification, attributes);

            if(match != null) {
              for(MediaDescriptor mediaDescriptor : match.b) {
                add(childStream, null, mediaDescriptor.getIdentifier(), match.a, mediaDescriptor);
              }

              continue nextChild;
            }
          }

          add(childStream, null, null, null, null);
        }
      }
    }
  }

  synchronized void remove(BasicStream stream) {
    for(BasicStream childStream : stream.getChildren()) {
      remove(childStream);
    }

    byUri.remove(stream.getUri());
    streams.remove(stream.getId());
    streamSources.remove(stream.getId());

    StreamID id = stream.getId();

    bmm.getRightSet(id).stream().forEach(identifier -> bmm.unassociate(id, identifier));
  }

  public synchronized Set<StreamID> findStreamIDs(Identifier identifier) {
    return bmm.getLeftMap(identifier).keySet();
  }

  public synchronized Set<Identifier> findIdentifiers(StreamID streamId) {
    return bmm.getRightMap(streamId).keySet();
  }

  // Provides consistent view
  public synchronized Set<BasicStream> findStreams(Identifier identifier) {
    return bmm.getLeftMap(identifier).keySet().stream()
      .map(streamId -> streams.get(streamId))
      .collect(Collectors.toSet());
  }

  public synchronized BasicStream findStream(StreamID streamId) {
    return streams.get(streamId);
  }

  public synchronized Map<Identifier, Tuple2<Identification, MediaDescriptor>> findDescriptorsAndIdentifications(StreamID streamId) {
    return bmm.getRightMap(streamId);
  }

  public synchronized Set<BasicStream> findStreams(MediaType type) {
    return streams.values().stream()
      .filter(s -> s.getType().equals(type))
      .collect(Collectors.toSet());
  }

  public synchronized Set<BasicStream> findStreams(String tag) {
    return streamSources.entrySet().stream()
      .filter(e -> e.getValue().getTags().contains(tag))
      .map(e -> streams.get(e.getKey()))
      .collect(Collectors.toSet());
  }

  public synchronized Map<BasicStream, Map<Identifier, Tuple2<Identification, MediaDescriptor>>> findAllDescriptorsAndIdentifications(MediaType type) {
    return streams.values().stream()
      .filter(s -> s.getType().equals(type))
      .collect(Collectors.toMap(Function.identity(), s -> findDescriptorsAndIdentifications(s.getId())));
  }

  public synchronized Map<BasicStream, Map<Identifier, Tuple2<Identification, MediaDescriptor>>> findAllDescriptorsAndIdentifications(MediaType type, String tag) {
    return streamSources.entrySet().stream()
      .filter(e -> e.getValue().getTags().contains(tag))
      .map(e -> streams.get(e.getKey()))
      .filter(s -> s.getType().equals(type))
      .collect(Collectors.toMap(Function.identity(), s -> findDescriptorsAndIdentifications(s.getId())));
  }

  public synchronized StreamSource findStreamSource(StreamID streamId) {
    return streamSources.get(streamId);
  }
}
