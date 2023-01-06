package hs.mediasystem.db.core;

import hs.ddif.annotations.Produces;
import hs.mediasystem.db.extract.StreamMetaDataEvent;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.MediaStructure;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.util.events.EventSelector;
import hs.mediasystem.util.events.streams.Source;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamablesAsResourceService {

  @Inject private EventSelector<StreamableEvent> streamableEvents;
  @Inject private Source<StreamMetaDataEvent> streamMetaDataEvents;

  private final Map<URI, Resource> resources = new HashMap<>();
  private final Map<ContentID, Set<URI>> locationsByContentId = new HashMap<>();
  private final Map<ContentID, StreamMetaData> metaDataByContentId = new HashMap<>();

  private Source<ResourceEvent> source;

  @PostConstruct
  private void postConstruct() {
    this.source = streamableEvents
      .plain()
      .map(e -> (Object)e)
      .mergeWith(streamMetaDataEvents)
      .mapMulti(this::processEvent);
  }

  @Produces
  private Source<ResourceEvent> resourceEvents() {
    return source;
  }

  private void processEvent(Object event, Consumer<ResourceEvent> output) {
    if(event instanceof StreamableEvent se) {
      processEvent(se, output);
    }
    else if(event instanceof StreamMetaDataEvent smde) {
      processEvent(smde, output);
    }
  }

  private void processEvent(StreamableEvent event, Consumer<ResourceEvent> output) {
    synchronized(resources) {
      URI location = event.location();

      if(event instanceof StreamableEvent.Updated u) {
        Streamable streamable = u.streamable();
        Resource existing = resources.get(location);
        Resource resource = existing == null ? create(streamable) : merge(existing, create(streamable));

        StreamMetaData smd = metaDataByContentId.get(streamable.contentId());

        if(smd != null) {
          resource = merge(resource, smd);
        }

        locationsByContentId.computeIfAbsent(streamable.contentId(), k -> new LinkedHashSet<>()).add(location);

        update(resource, output);
      }
      else if(event instanceof StreamableEvent.Removed r) {
        Resource existing = resources.get(location);

        locationsByContentId.computeIfPresent(existing.contentId(), (k, v) -> v.remove(location) && v.isEmpty() ? null : v);
        resources.remove(location);

        output.accept(new ResourceEvent.Removed(location));
      }
    }
  }

  private void processEvent(StreamMetaDataEvent event, Consumer<ResourceEvent> output) {
    ContentID contentId = event.id();

    synchronized(resources) {
      if(event instanceof StreamMetaDataEvent.Updated u) {
        metaDataByContentId.put(contentId, u.streamMetaData());

        updateLinkedResources(contentId, u.streamMetaData(), output);
      }
      else if(event instanceof StreamMetaDataEvent.Removed r) {
        metaDataByContentId.remove(contentId);

        updateLinkedResources(contentId, null, output);
      }
    }
  }

  private void updateLinkedResources(ContentID contentId, StreamMetaData streamMetaData, Consumer<ResourceEvent> output) {
    // No concurrent modification exception here because we are only updating keys
    locationsByContentId.getOrDefault(contentId, Set.of()).forEach(location -> update(merge(resources.get(location), streamMetaData), output));
  }

  private void update(Resource resource, Consumer<ResourceEvent> output) {
    resources.put(resource.location(), resource);

    output.accept(new ResourceEvent.Updated(resource));
  }

  private static Resource create(Streamable streamable) {
    ContentPrint contentPrint = streamable.discovery().contentPrint();

    return new Resource(
      streamable.location(),
      streamable.discovery().parentLocation(),
      streamable.discovery().mediaType(),
      streamable.contentId(),
      Instant.ofEpochMilli(contentPrint.getLastModificationTime()),
      Optional.ofNullable(contentPrint.getSize()),
      contentPrint.getSignatureCreationTime(),
      streamable.tags(),
      Optional.empty(),
      Optional.empty(),
      List.of(),
      streamable.discovery().attributes()
    );
  }

  private static Resource merge(Resource resource, Resource updated) {
    return new Resource(
      updated.location(),
      updated.parentLocation(),
      updated.mediaType(),
      updated.contentId(),
      updated.lastModificationTime(),
      updated.size(),
      updated.discoveryTime(),
      updated.tags(),
      resource.duration(),
      resource.mediaStructure(),
      resource.snapshots(),
      updated.attributes()
    );
  }

  private static Resource merge(Resource resource, StreamMetaData smd) {
    return new Resource(
      resource.location(),
      resource.parentLocation(),
      resource.mediaType(),
      resource.contentId(),
      resource.lastModificationTime(),
      resource.size(),
      resource.discoveryTime(),
      resource.tags(),
      smd == null ? Optional.empty() : smd.length(),
      Optional.ofNullable(smd == null ? null : new MediaStructure(smd.videoTracks(), smd.audioTracks(), smd.subtitleTracks())),
      smd == null ? List.of() : smd.snapshots(),
      resource.attributes()
    );
  }
}
