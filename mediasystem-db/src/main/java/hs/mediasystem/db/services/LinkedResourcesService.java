package hs.mediasystem.db.services;

import hs.ddif.annotations.Produces;
import hs.mediasystem.db.base.CachedStream;
import hs.mediasystem.db.base.ImportSourceProvider;
import hs.mediasystem.db.base.StreamableEvent;
import hs.mediasystem.db.extract.StreamMetaDataEvent;
import hs.mediasystem.db.services.domain.LinkedResource;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.db.services.domain.Work;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.MediaStructure;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.util.events.EventSource;
import hs.mediasystem.util.events.EventStream;
import hs.mediasystem.util.events.InMemoryEventStore;
import hs.mediasystem.util.events.SimpleEventStream;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LinkedResourcesService {

  /**
   * DataSource which uses as key a StreamID.<p>
   *
   * This internal data source is used for items that are part of a serie but have
   * not been matched up to a known episode or special.
   */
  private static final String DEFAULT_DATA_SOURCE_NAME = "@INTERNAL";
  private static final MultiMapper<StreamID, LinkedResource> MAPPER = new MultiMapper<>(LinkedResource::id);

  @Inject private DescriptorStore descriptorStore;
  @Inject private EventSource<StreamableEvent> streamableEvents;
  @Inject private EventSource<StreamMetaDataEvent> streamMetaDataEvents;
  @Inject private ImportSourceProvider importSourceProvider;
  @Inject private ContentPrintProvider contentPrintProvider;

  private final Map<ContentID, Object> linkedResourcesByContentId = new HashMap<>();
  private final Map<ContentID, StreamMetaData> metaDataByContentId = new HashMap<>();
  private final EventStream<LinkedResourceEvent> eventStream = new SimpleEventStream<>(new InMemoryEventStore<>());

  @Inject
  private LinkedResourcesService() {
  }

  @PostConstruct
  private void postConstruct() {
    streamableEvents.subscribeAndWait(this::processEvent);
    streamMetaDataEvents.subscribeAndWait(this::processStreamMetaDataEvent);
  }

  @Produces
  private EventSource<LinkedResourceEvent> linkedResourceEvents() {
    return eventStream;
  }

  /**
   * Returns the first resource matching the given {@link ContentID}.
   *
   * @param cid a {@link ContentID}, cannot be {@code null}
   * @return an optional {@link LinkedResource}, never {@code null}
   */
  public Optional<LinkedResource> findFirst(ContentID cid) {
    synchronized(linkedResourcesByContentId) {
      return MAPPER.findFirst(linkedResourcesByContentId.get(cid));
    }
  }

  private void processEvent(StreamableEvent event) {
    synchronized(linkedResourcesByContentId) {
      if(event instanceof StreamableEvent.Updated u) {
        CachedStream cs = u.cachedStream();
        LinkedResource resource = toStream(cs);

        linkedResourcesByContentId.compute(cs.getStreamable().getId().getContentId(), (k, v) -> MAPPER.put(v, resource));

        eventStream.push(new LinkedResourceEvent.Updated(resource));
      }
      else if(event instanceof StreamableEvent.Removed r) {
        linkedResourcesByContentId.compute(r.id().getContentId(), (k, v) -> MAPPER.remove(v, r.id()));

        eventStream.push(new LinkedResourceEvent.Removed(r.id()));
      }
    }
  }

  private void processStreamMetaDataEvent(StreamMetaDataEvent event) {
    synchronized(linkedResourcesByContentId) {
      if(event instanceof StreamMetaDataEvent.Updated u) {
        ContentID contentId = u.streamMetaData().contentId();

        metaDataByContentId.put(contentId, u.streamMetaData());

        updateLinkedResources(contentId, u.streamMetaData());
      }
      else if(event instanceof StreamMetaDataEvent.Removed r) {
        ContentID contentId = r.id();

        metaDataByContentId.remove(contentId);

        updateLinkedResources(contentId, null);
      }
    }
  }

  private void updateLinkedResources(ContentID contentId, StreamMetaData streamMetaData) {
    // No concurrent modification exception here because we are only updating keys
    MAPPER.stream(linkedResourcesByContentId.get(contentId)).forEach(lr -> {
      LinkedResource merged = merge(lr, streamMetaData);

      linkedResourcesByContentId.compute(contentId, (k, v) -> MAPPER.put(v, merged));

      eventStream.push(new LinkedResourceEvent.Updated(merged));
    });
  }

  private LinkedResource toStream(CachedStream cs) {
    Streamable streamable = cs.getStreamable();
    StreamID id = streamable.getId();
    ContentID contentId = id.getContentId();
    ContentPrint contentPrint = contentPrintProvider.get(contentId);

    LinkedResource lr = new LinkedResource(
      new Resource(
        id,
        streamable.getParentId(),
        streamable.getUri(),
        streamable.getAttributes(),
        cs.getDiscoveryTime(),
        Instant.ofEpochMilli(contentPrint.getLastModificationTime()),
        Optional.ofNullable(contentPrint.getSize()),
        importSourceProvider.getImportSource(id.getImportSourceId()).getStreamSource().tags(),
        Optional.empty(),
        Optional.empty(),
        List.of()
      ),
      cs.getIdentification().map(Identification::getMatch).orElse(new Match(Type.NONE, 1.0f, cs.getDiscoveryTime())),
      createWorks(cs)
    );

    return Optional.ofNullable(metaDataByContentId.get(contentId)).map(md -> merge(lr, md)).orElse(lr);
  }

  private static LinkedResource merge(LinkedResource lr, StreamMetaData smd) {
    Resource resource = lr.resource();

    return new LinkedResource(
      new Resource(
        resource.id(),
        resource.parentId(),
        resource.uri(),
        resource.attributes(),
        resource.discoveryTime(),
        resource.lastModificationTime(),
        resource.size(),
        resource.tags(),
        smd == null ? Optional.empty() : smd.length(),
        Optional.ofNullable(smd == null ? null : new MediaStructure(smd.videoTracks(), smd.audioTracks(), smd.subtitleTracks())),
        smd == null ? List.of() : smd.snapshots()
      ),
      lr.match(),
      lr.works()
    );
  }

  private List<Work> createWorks(CachedStream cs) {
    cs.getStreamable().getParentId();

    List<Work> works = cs.getIdentification()
      .map(Identification::getWorkIds)
      .stream()
      .flatMap(Collection::stream)
      .map(descriptorStore::find)
      .flatMap(Optional::stream)
      .map(d -> new Work(
        d,
        findParent(d).or(() -> cs.getStreamable().getParentId().flatMap(this::createParent))
      ))
      .toList();

    return works.isEmpty() ? List.of(createMinimalWork(cs)) : works;
  }

  private Work createMinimalWork(CachedStream cs) {
    WorkDescriptor descriptor = createMinimalDescriptor(cs.getStreamable());

    return new Work(
      descriptor,
      cs.getStreamable().getParentId().flatMap(this::createParent)
    );
  }

  private Optional<Parent> findParent(WorkDescriptor descriptor) {
    if(descriptor instanceof Movie movie) {
      return movie.getCollectionId()
        .flatMap(descriptorStore::find)
        .map(this::createParent);
    }

    if(descriptor instanceof Episode) {
      return descriptor.getId().getParent().flatMap(descriptorStore::find)
        .map(this::createParent);
    }

    return Optional.empty();
  }

  private Optional<Parent> createParent(StreamID parentId) {
    LinkedResource parent = MAPPER.get(linkedResourcesByContentId.get(parentId.getContentId()), parentId);

    if(parent == null || parent.works().isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(createParent(parent.works().get(0).descriptor()));
  }

  private Parent createParent(WorkDescriptor descriptor) {
    Details details = descriptor.getDetails();

    return new Parent(descriptor.getId(), details.getTitle(), details.getBackdrop());
  }

  private static WorkDescriptor createMinimalDescriptor(Streamable streamable) {
    return new Production(
      new WorkId(DataSource.instance(DEFAULT_DATA_SOURCE_NAME), streamable.getType(), "" + streamable.getId().asString()),
      createMinimalDetails(streamable),
      null,
      null,
      Classification.EMPTY,
      0.0,
      Set.of()
    );
  }

  private static Details createMinimalDetails(Streamable streamable) {
    String title = streamable.getAttributes().get(Attribute.TITLE);
    String subtitle = streamable.getAttributes().get(Attribute.SUBTITLE);

    if(title == null || title.isBlank()) {
      title = subtitle;
      subtitle = null;
    }

    if(title == null || title.isBlank()) {
      title = "(Untitled)";
    }

    return new Details(
      title,
      subtitle,
      streamable.getAttributes().get(Attribute.DESCRIPTION),
      null,
      null,  // no images derived from stream image captures are provided, front-end should do appropriate fall backs
      null,
      null
    );
  }

  /**
   * Helper class that simplifies tracking zero, one or more key value pairs with minimal overhead.
   * The helper requires the current base value for all its functions. When modified the mapper
   * returns the new base value, which can be {@code null} if there are no key value pairs, just
   * a value if there is a single key value pair, or a {@code Map} if there are multiple pairs.
   *
   * @param <K> the type of the keys
   * @param <V> the type of the values
   */
  static class MultiMapper<K, V> {
    private final Function<V, K> keyExtractor;

    public MultiMapper(Function<V, K> keyExtractor) {
      this.keyExtractor = keyExtractor;
    }

    /**
     * Returns the value associated with the given key, or {@code null} if there is none.
     *
     * @param base a base to examine, can be {@code null}
     * @param key a key, can be {@code null}
     * @return the value associated with the given key, or {@code null} if there is none
     */
    public V get(Object base, K key) {
      if(base == null) {
        return null;
      }

      if(base instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<K, V> map = (Map<K, V>)base;

        return map.get(key);
      }

      @SuppressWarnings("unchecked")
      V baseValue = (V)base;
      K baseKey = keyExtractor.apply(baseValue);

      return baseKey.equals(key) ? baseValue : null;
    }

    @SuppressWarnings("unchecked")
    public Optional<V> findFirst(Object base) {
      return Optional.ofNullable(base == null ? null : base instanceof Map ? ((Map<K, V>)base).values().iterator().next() : (V)base);
    }

    @SuppressWarnings("unchecked")
    public Stream<V> stream(Object base) {
      return base == null ? Stream.empty() : base instanceof Map ? ((Map<K, V>)base).values().stream() : Stream.of((V)base);
    }

    public Object put(Object base, V value) {
      if(base == null) {
        return value;
      }

      K key = keyExtractor.apply(value);

      if(base instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<K, V> map = (Map<K, V>)base;

        map.put(key, value);

        return base;
      }

      @SuppressWarnings("unchecked")
      K baseKey = keyExtractor.apply((V)base);

      return key.equals(baseKey) ? value : new HashMap<>(Map.of(baseKey, base, key, value));
    }

    public Object remove(Object base, K key) {
      if(base == null) {
        return null;
      }

      if(base instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<K, V> map = (Map<K, V>)base;

        map.remove(key);

        return map.size() == 1 ? map.values().iterator().next() : map;
      }

      @SuppressWarnings("unchecked")
      K baseKey = keyExtractor.apply((V)base);

      return baseKey.equals(key) ? null : base;
    }
  }
}
