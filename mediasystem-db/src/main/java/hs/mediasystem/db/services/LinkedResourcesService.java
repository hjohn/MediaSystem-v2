package hs.mediasystem.db.services;

import hs.ddif.annotations.Produces;
import hs.mediasystem.db.base.CachedStream;
import hs.mediasystem.db.base.ImportSourceProvider;
import hs.mediasystem.db.base.StreamableEvent;
import hs.mediasystem.db.services.domain.LinkedResource;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.db.services.domain.Work;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.Parent;
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
import hs.mediasystem.util.events.Event;
import hs.mediasystem.util.events.EventSource;
import hs.mediasystem.util.events.EventStream;
import hs.mediasystem.util.events.InMemoryEventStream;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

// TODO Could subscribe to StreamMetaData events and use it to enhance LocalResource

@Singleton
public class LinkedResourcesService {

  /**
   * DataSource which uses as key a StreamID.<p>
   *
   * This internal data source is used for items that are part of a serie but have
   * not been matched up to a known episode or special.
   */
  private static final String DEFAULT_DATA_SOURCE_NAME = "@INTERNAL";

  @Inject private DescriptorStore descriptorStore;
  @Inject private EventSource<StreamableEvent> streamableEvents;
  @Inject private ImportSourceProvider importSourceProvider;
  @Inject private ContentPrintProvider contentPrintProvider;

  private final Map<StreamID, LinkedResource> linkedResources = new HashMap<>();
  private final EventStream<LinkedResourceEvent> eventStream = new InMemoryEventStream<>();

  @Inject
  private LinkedResourcesService() {
  }

  @PostConstruct
  private void postConstruct() {
    streamableEvents.subscribeAndWait(this::processEvent);
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
    synchronized(linkedResources) {
      return linkedResources.values().stream()
        .filter(lr -> lr.id().getContentId().equals(cid))
        .findFirst();
    }
  }

  private void processEvent(StreamableEvent event) {
    synchronized(linkedResources) {
      if(event instanceof StreamableEvent.Updated u) {
        CachedStream cs = u.cachedStream();
        LinkedResource resource = toStream(cs);

        linkedResources.put(cs.getStreamable().getId(), resource);

        eventStream.push(new Event<>(new LinkedResourceEvent.Updated(resource)));
      }
      else if(event instanceof StreamableEvent.Removed r) {
        linkedResources.remove(r.id());

        eventStream.push(new Event<>(new LinkedResourceEvent.Removed(r.id())));
      }
    }
  }

  private LinkedResource toStream(CachedStream cs) {
    ContentPrint contentPrint = contentPrintProvider.get(cs.getStreamable().getId().getContentId());

    return new LinkedResource(
      new Resource(
        cs.getStreamable().getId(),
        cs.getStreamable().getParentId(),
        cs.getStreamable().getType(),
        cs.getStreamable().getUri(),
        cs.getStreamable().getAttributes(),
        cs.getDiscoveryTime(),
        Instant.ofEpochMilli(contentPrint.getLastModificationTime()),
        Optional.ofNullable(contentPrint.getSize()),
        importSourceProvider.getImportSource(cs.getStreamable().getId().getImportSourceId()).getStreamSource().getTags()
      ),
      cs.getIdentification().map(Identification::getMatch).orElse(new Match(Type.NONE, 1.0f, cs.getDiscoveryTime())),
      createWorks(cs)
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
    LinkedResource parent = linkedResources.get(parentId);

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
}
