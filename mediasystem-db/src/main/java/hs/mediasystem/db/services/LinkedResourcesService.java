package hs.mediasystem.db.services;

import hs.ddif.annotations.Produces;
import hs.mediasystem.db.core.IdentificationEvent;
import hs.mediasystem.db.core.ResourceEvent;
import hs.mediasystem.db.services.domain.LinkedResource;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.db.services.domain.Work;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.events.InMemoryEventStore;
import hs.mediasystem.util.events.EventSelector;
import hs.mediasystem.util.events.SimpleEventStream;
import hs.mediasystem.util.events.streams.Source;
import hs.mediasystem.util.events.streams.EventStream;
import hs.mediasystem.util.events.streams.Subscription;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LinkedResourcesService {

  /**
   * DataSource which uses as key a {@link URI}.<p>
   *
   * This internal data source is used for items that are part of a serie but have
   * not been matched up to a known episode or special.
   */
  private static final String DEFAULT_DATA_SOURCE_NAME = "@INTERNAL";

  @Inject private Source<ResourceEvent> resourceEvents;
  @Inject private EventSelector<IdentificationEvent> identificationEvents;

  private final NavigableMap<URI, LinkedResource> linkedResources = new TreeMap<>();
  private final Map<URI, URI> parents = new HashMap<>();
  private final Map<ContentID, Set<URI>> locationsByContentId = new HashMap<>();
  private final EventStream<LinkedResourceEvent> eventStream = new SimpleEventStream<>(new InMemoryEventStore<>(LinkedResourceEvent.class));

  private Subscription resourceSubscription;

  @Inject
  private LinkedResourcesService() {
  }

  @PostConstruct
  private void postConstruct() {
    this.resourceSubscription = resourceEvents.subscribe("LinkedResourcesService", this::processEvent);
    this.resourceSubscription.join();

    identificationEvents.plain().subscribe("LinkedResourcesService", this::processEvent).join();
  }

  @Produces
  private Source<LinkedResourceEvent> linkedResourceEvents() {
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
      return locationsByContentId.getOrDefault(cid, Set.of()).stream().map(linkedResources::get).findFirst();
    }
  }

  public Optional<LinkedResource> findParent(URI uri) {
    synchronized(linkedResources) {
      return Optional.of(uri).map(parents::get).map(linkedResources::get);
    }
  }

  public Optional<LinkedResource> find(URI uri) {
    synchronized(linkedResources) {
      return Optional.ofNullable(linkedResources.get(uri));
    }
  }

  private void processEvent(ResourceEvent event) {
    synchronized(linkedResources) {
      URI location = event.location();

      if(event instanceof ResourceEvent.Updated u) {
        Resource resource = u.resource();
        LinkedResource existing = linkedResources.get(location);
        LinkedResource linkedResource = existing == null ? create(resource) : merge(existing, create(resource));

        locationsByContentId.computeIfAbsent(resource.contentId(), k -> new LinkedHashSet<>()).add(location); // TODO could use different structure than set if we check existing == null first

        update(linkedResource);
      }
      else if(event instanceof ResourceEvent.Removed r) {
        LinkedResource existing = linkedResources.get(location);

        locationsByContentId.computeIfPresent(existing.contentId(), (k, v) -> v.remove(location) && v.isEmpty() ? null : v);
        linkedResources.remove(location);
        parents.remove(location);

        eventStream.push(new LinkedResourceEvent.Removed(location));
      }
    }
  }

  private void processEvent(IdentificationEvent event) {
    resourceSubscription.join();  // ensures identifications are never processed before its corresponding resource is known

    synchronized(linkedResources) {
      LinkedResource existing = linkedResources.get(event.location());

      if(existing != null) {
        LinkedResource resource = merge(existing, new IdentificationService.Identification(event.descriptors(), event.match()));

        update(resource);
      }
    }
  }

  private void update(LinkedResource linkedResource) {
    linkedResources.put(linkedResource.location(), linkedResource);
    parents.remove(linkedResource.location());
    linkedResource.resource().parentLocation().ifPresent(parentLocation -> parents.put(linkedResource.location(), parentLocation));

    eventStream.push(new LinkedResourceEvent.Updated(linkedResource));
  }

  private static LinkedResource create(Resource resource) {
    return new LinkedResource(
      resource,
      new Match(Type.NONE, 1.0f, resource.discoveryTime()),
      createWorks(resource, Optional.empty())
    );
  }

  private static LinkedResource merge(LinkedResource lr, LinkedResource updated) {
    return new LinkedResource(
      updated.resource(),
      lr.match(),
      lr.works()
    );
  }

  private static LinkedResource merge(LinkedResource lr, IdentificationService.Identification mi) {
    Resource resource = lr.resource();

    return new LinkedResource(
      resource,
      mi.match(),
      createWorks(resource, Optional.of(mi))
    );
  }

  private static List<Work> createWorks(Resource resource, Optional<IdentificationService.Identification> mi) {
    List<Work> works = mi.map(IdentificationService.Identification::descriptors)
      .stream()
      .flatMap(Collection::stream)
      .map(Work::new)
      .toList();

    return works.isEmpty() ? List.of(createMinimalWork(resource)) : works;
  }

  private static Work createMinimalWork(Resource resource) {
    WorkDescriptor descriptor = createMinimalDescriptor(resource);

    return new Work(descriptor);
  }

  private static WorkDescriptor createMinimalDescriptor(Resource resource) {
    return new Production(
      new WorkId(DataSource.instance(DEFAULT_DATA_SOURCE_NAME), resource.mediaType(), resource.location().toString()),  // TODO slash not allowed in location.. there is a trick to determine parent there
      createMinimalDetails(resource.attributes()),
      null,
      null,
      Classification.EMPTY,
      0.0,
      Set.of()
    );
  }

  private static Details createMinimalDetails(Attributes attributes) {
    String title = attributes.get(Attribute.TITLE);
    String subtitle = attributes.get(Attribute.SUBTITLE);

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
      attributes.get(Attribute.DESCRIPTION),
      null,
      null,  // no images derived from stream image captures are provided, front-end should do appropriate fall backs
      null,
      null
    );
  }
}
