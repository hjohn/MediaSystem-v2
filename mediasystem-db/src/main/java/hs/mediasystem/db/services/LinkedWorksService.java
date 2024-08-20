package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.domain.Release;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.events.streams.Source;

import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service providing {@link LinkedWork}s.
 */
@Singleton
class LinkedWorksService {
  private static final DataSource RESOURCE_DATA_SOURCE = DataSource.instance("@INTERNAL");
  private static final Comparator<LinkedWork> REVERSED_CREATION_ORDER = Comparator
    .comparing((LinkedWork lw) -> lw.resources().getFirst().streamable().contentPrint().getSignatureCreationTime())
    .thenComparing(lw -> lw.resources().getFirst().streamable().contentPrint().getLastModificationTime())
    .reversed();

  private final Map<WorkId, LinkedWork> linkedWorks = new HashMap<>();
  private final Map<URI, Set<URI>> children = new HashMap<>();
  private final Map<URI, Set<WorkId>> workIdsByLocation = new HashMap<>();
  private final Map<URI, URI> parentLocations = new HashMap<>();  // TODO unused, remove

  @Inject
  private LinkedWorksService(Source<ResourceEvent> resourceEvents) {
    resourceEvents.subscribe(this::processEvent).join();
  }

  private void processEvent(ResourceEvent event) {
    synchronized(linkedWorks) {
      if(event instanceof ResourceEvent.Updated u) {
        Resource resource = u.resource();

        remove(resource.location());
        add(resource);
      }
      else if(event instanceof ResourceEvent.Removed r) {
        remove(r.location());
      }
    }
  }

  private void add(Resource resource) {
    addStream(resource);
    addResourceToChildren(resource);
  }

  private void remove(URI location) {
    removeResourceFromChildren(location);
    removeStream(location);
  }

  private void addResourceToChildren(Resource resource) {
    resource.streamable().parentLocation().ifPresent(parentLocation -> {
      parentLocations.put(resource.location(), parentLocation);
      children.computeIfAbsent(parentLocation, k -> new HashSet<>()).add(resource.location());
    });
  }

  private void removeResourceFromChildren(URI location) {
    URI parentLocation = parentLocations.remove(location);

    if(parentLocation != null) {
      children.computeIfPresent(parentLocation, (k, v) -> v.remove(location) && v.isEmpty() ? null : v);
    }
  }

  private void addStream(Resource resource) {
    for(Release release : resource.releases()) {
      LinkedWork linkedWork = new LinkedWork(release, List.of(resource));

      linkedWorks.merge(linkedWork.id(), linkedWork, (w1, w2) -> new LinkedWork(
        w2.workDescriptor(),
        Stream.concat(w1.resources().stream(), w2.resources().stream()).toList()
      ));
    }

    workIdsByLocation.put(resource.location(), resource.releases().stream().map(Release::getId).collect(Collectors.toSet()));
  }

  private void removeStream(URI location) {
    Set<WorkId> affectedWorkIds = workIdsByLocation.remove(location);

    if(affectedWorkIds != null) {
      for(WorkId workId : affectedWorkIds) {
        LinkedWork linkedWork = linkedWorks.get(workId);  // should never return null as index is always maintained

        if(linkedWork.resources().size() == 1) {
          linkedWorks.remove(linkedWork.id());
        }
        else {
          linkedWorks.put(linkedWork.id(), new LinkedWork(
            linkedWork.workDescriptor(),
            linkedWork.resources().stream().filter(mr -> !mr.location().equals(location)).toList()
          ));
        }
      }
    }
  }

  /**
   * Finds a {@link LinkedWork} by id.
   *
   * @param id a {@link WorkId}, cannot be {@code null}
   * @return an optional {@link LinkedWork}, never {@code null}
   */
  public Optional<LinkedWork> find(WorkId id) {
    synchronized(linkedWorks) {
      return Optional.ofNullable(linkedWorks.get(id))
        .or(() -> id.getDataSource().equals(RESOURCE_DATA_SOURCE) ? find(URI.create(id.getKey())).stream().findFirst() : Optional.empty());
    }
  }

  /**
   * Finds the children of the work given by the id.
   *
   * @param id a {@link WorkId}, cannot be {@code null}
   * @return a list of {@link LinkedWork}s, never {@code null} or contains {@code null}s, but can be empty
   */
  public List<LinkedWork> findChildren(WorkId id) {
    synchronized(linkedWorks) {
      return find(id).stream()
        .map(LinkedWork::resources)
        .flatMap(List::stream)
        .map(Resource::location)
        .map(children::get)
        .filter(Objects::nonNull)  // children are not added immediately, and can be unavailable momentarily
        .flatMap(Set::stream)
        .map(workIdsByLocation::get)
        .flatMap(Set::stream)
        .distinct()
        .map(linkedWorks::get)
        .toList();
    }
  }

  /**
   * Finds the newest works matching the given {@link MediaType} filter up to the given maximum.
   *
   * @param maximum the maximum number of works to find, cannot be negative
   * @param filter a {@link MediaType} filter, cannot be {@code null}
   * @return a list of {@link LinkedWork}s, never {@code null} or contains {@code null}s, but can be empty
   */
  public List<LinkedWork> findNewest(int maximum, Predicate<MediaType> filter) {
    synchronized(linkedWorks) {
      return linkedWorks.values().stream()
        .filter(lw -> filter.test(lw.id().getType()))
        .sorted(REVERSED_CREATION_ORDER)
        .limit(maximum)
        .collect(Collectors.toList());
    }
  }

  /**
   * Returns linked works with the given tag that have no parent (roots).
   *
   * @param tag a required tag, or {@code null} for all tags
   * @return a list of {@link LinkedWork}s, never {@code null} or contains {@code null}s, but can be empty
   */
  public List<LinkedWork> findRootsByTag(String tag) {
    // This currently allows a work to have two resources with a different parent; no idea how that could be achieved though
    // Perhaps if there are two serie folders, that both identify to the same serie, and there is an episode in each of these folders that is the same work
    // Can't think of a situation where one of the resources would have a parent, but the other doesn't (probably impossible, but would like to enforce this then)
    // TODO this is a bit clumsy, and mainly used for Folder/File. If top level folders had a special tag or MediaType, this would be much easier.
    synchronized(linkedWorks) {
      return linkedWorks.values().stream()
        .filter(lw -> tag == null || hasTag(lw, tag))
        .filter(lw -> !lw.resources().stream().flatMap(r -> r.streamable().parentLocation().stream()).anyMatch(Objects::nonNull))
        .collect(Collectors.toList());
    }
  }

  /**
   * Returns linked works of the given type with the given.
   *
   * @param type a required {@link MediaType}, or {@code null} for all types
   * @param tag a required tag, or {@code null} for all tags
   * @return a list of {@link LinkedWork}s, never {@code null} or contains {@code null}s, but can be empty
   */
  public List<LinkedWork> findAllByType(MediaType type, String tag) {
    synchronized(linkedWorks) {
      return linkedWorks.values().stream()
        .filter(lw -> type == null || lw.id().getType() == type)
        .filter(lw -> tag == null || hasTag(lw, tag))
        .collect(Collectors.toList());
    }
  }

  /**
   * Returns linked works associated with the given stream in their logical order. More
   * than one work can be returned in cases where a stream contains multiple logical
   * works, like a two part episode contained in one stream.
   *
   * @param location a {@link URI}, cannot be {@code null}
   * @return a list of {@link LinkedWork}s, never {@code null} or contains {@code null}s, but can be empty
   */
  public List<LinkedWork> find(URI location) {
    synchronized(linkedWorks) {
      return workIdsByLocation.getOrDefault(location, Set.of()).stream()
        .map(linkedWorks::get)
        .sorted(Comparator.comparing(lw -> lw.id().toString()))
        .collect(Collectors.toList());
    }
  }

  /**
   * Returns linked works associated with the given content in their logical order. More
   * than one work can be returned in cases where multiple streams with the same content
   * identify as different works.
   *
   * @param cid a {@link ContentID}, cannot be {@code null}
   * @return a list of {@link LinkedWork}s, never {@code null} or contains {@code null}s, but can be empty
   */
  public List<LinkedWork> find(ContentID cid) {
    synchronized(linkedWorks) {
      return linkedWorks.values().stream()
        .filter(lw -> lw.resources().stream().anyMatch(mr -> mr.contentId().equals(cid)))
        .sorted(Comparator.comparing(lw -> lw.id().toString()))
        .collect(Collectors.toList());
    }
  }

  private static boolean hasTag(LinkedWork linkedWork, String tag) {
    return linkedWork.resources().getFirst().streamable().tags().contains(tag);
  }
}
