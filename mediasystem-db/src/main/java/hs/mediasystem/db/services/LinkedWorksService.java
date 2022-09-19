package hs.mediasystem.db.services;

import hs.mediasystem.db.services.domain.LinkedResource;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.MatchedResource;
import hs.mediasystem.db.services.domain.Work;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.events.EventSource;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private static final Comparator<LinkedWork> REVERSED_CREATION_ORDER = Comparator
    .comparing((LinkedWork lw) -> lw.matchedResources().get(0).resource().discoveryTime())
    .thenComparing(lw -> lw.matchedResources().get(0).resource().lastModificationTime())
    .reversed();

  private final Map<WorkId, LinkedWork> linkedWorks = new HashMap<>();
  private final Map<StreamID, Set<WorkId>> workIdsByStreamId = new HashMap<>();

  @Inject
  private LinkedWorksService(EventSource<LinkedResourceEvent> linkedResourceEvents) {
    linkedResourceEvents.subscribeAndWait(this::processEvent);
  }

  private void processEvent(LinkedResourceEvent event) {
    synchronized(linkedWorks) {
      if(event instanceof LinkedResourceEvent.Updated u) {
        LinkedResource linkedResource = u.resource();

        removeStream(linkedResource.id());

        for(Work work : linkedResource.works()) {
          LinkedWork linkedWork = new LinkedWork(work, List.of(new MatchedResource(linkedResource.match(), linkedResource.resource())));

          linkedWorks.merge(linkedWork.id(), linkedWork, (w1, w2) -> new LinkedWork(
            w2.work(),
            Stream.concat(w1.matchedResources().stream(), w2.matchedResources().stream()).toList()
          ));
        }

        workIdsByStreamId.put(linkedResource.id(), linkedResource.works().stream().map(Work::id).collect(Collectors.toSet()));
      }
      else if(event instanceof LinkedResourceEvent.Removed r) {
        removeStream(r.id());
      }
    }
  }

  private void removeStream(StreamID sid) {
    synchronized(linkedWorks) {
      Set<WorkId> affectedWorkIds = workIdsByStreamId.remove(sid);

      if(affectedWorkIds != null) {
        for(WorkId workId : affectedWorkIds) {
          LinkedWork linkedWork = linkedWorks.get(workId);  // should never return null as index is always maintained

          if(linkedWork.matchedResources().size() == 1) {
            linkedWorks.remove(linkedWork.id());
          }
          else {
            linkedWorks.put(linkedWork.id(), new LinkedWork(
              linkedWork.work(),
              linkedWork.matchedResources().stream().filter(mr -> !mr.resource().id().equals(sid)).toList()
            ));
          }
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
      return Optional.ofNullable(linkedWorks.get(id));
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
      return linkedWorks.values().stream()
        .filter(w -> id.equals(w.work().parent().map(Parent::id).orElse(null)))
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
    synchronized(linkedWorks) {
      return linkedWorks.values().stream()
        .filter(lw -> lw.work().parent().isEmpty())
        .filter(lw -> tag == null || hasTag(lw, tag))
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
   * @param sid a {@link StreamID}, cannot be {@code null}
   * @return a list of {@link LinkedWork}s, never {@code null} or contains {@code null}s, but can be empty
   */
  public List<LinkedWork> find(StreamID sid) {
    synchronized(linkedWorks) {
      return workIdsByStreamId.getOrDefault(sid, Set.of()).stream()
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
        .filter(lw -> lw.matchedResources().stream().anyMatch(mr -> mr.resource().id().getContentId().equals(cid)))
        .sorted(Comparator.comparing(lw -> lw.id().toString()))
        .collect(Collectors.toList());
    }
  }

  private static boolean hasTag(LinkedWork linkedWork, String tag) {
    return linkedWork.matchedResources().get(0).resource().tags().contains(tag);
  }
}
