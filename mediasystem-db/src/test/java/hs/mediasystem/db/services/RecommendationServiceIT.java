package hs.mediasystem.db.services;

import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.db.InjectorExtension;
import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.base.StreamStateStore;
import hs.mediasystem.db.core.DescriptorService;
import hs.mediasystem.db.core.IdentificationEvent;
import hs.mediasystem.db.core.ResourceEvent;
import hs.mediasystem.db.core.StreamTags;
import hs.mediasystem.db.extract.DefaultStreamMetaDataStore;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.events.EventSelector;
import hs.mediasystem.util.events.InMemoryEventStore;
import hs.mediasystem.util.events.PersistentEventStream;
import hs.mediasystem.util.events.SynchronousEventStream;
import hs.mediasystem.util.events.streams.EventStream;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.int4.dirk.annotations.Produces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(InjectorExtension.class)
public class RecommendationServiceIT {
  @Produces private static final EventStream<ResourceEvent> RESOURCE_EVENTS = new SynchronousEventStream<>();
  @Produces private static final EventSelector<IdentificationEvent> IDENTIFICATION_EVENTS = new PersistentEventStream<>(new InMemoryEventStore<>(IdentificationEvent.class));
  @Produces private static final StreamStateStore STREAM_STATE_STORE = mock(StreamStateStore.class);
  @Produces private static final DescriptorService DESCRIPTOR_SERVICE = mock(DescriptorService.class);
  @Produces private static final DefaultStreamMetaDataStore STREAM_META_DATA_STORE = mock(DefaultStreamMetaDataStore.class);

  @Inject private RecommendationService recommendationService;
  @Inject private StreamStateService streamStateService;

  @BeforeEach
  void beforeEach() {
    RESOURCE_EVENTS.push(new ResourceEvent.Updated(new Resource(
      Path.of("/Terminator.avi").toUri(),
      Optional.empty(),
      MediaType.MOVIE,
      new ContentID(1),
      Instant.now(),
      Optional.of(12345L),
      Instant.now(),
      new StreamTags(Set.of("cartoons")),
      Optional.empty(),
      Optional.empty(),
      List.of(),
      Attributes.of(Attribute.TITLE, "Terminator")
    )));

    RESOURCE_EVENTS.push(new ResourceEvent.Updated(new Resource(
      Path.of("/BTTF.avi").toUri(),
      Optional.empty(),
      MediaType.MOVIE,
      new ContentID(2),
      Instant.now(),
      Optional.of(12345L),
      Instant.now(),
      new StreamTags(Set.of("cartoons")),
      Optional.empty(),
      Optional.empty(),
      List.of(),
      Attributes.of(Attribute.TITLE, "Back to the Future")
    )));
  }

  @Nested
  class When_findRecommendations_IsCalled {
    {
      Instant now = Instant.now();

      streamStateService.setLastWatchedTime(new ContentID(1), now.minus(2, ChronoUnit.MINUTES));
      streamStateService.setResumePosition(new ContentID(1), 60);

      streamStateService.setLastWatchedTime(new ContentID(2), now.minus(2, ChronoUnit.DAYS));
      streamStateService.setResumePosition(new ContentID(2), 60);
    }

    @Test
    void shouldReturnExpectedResults() {
      assertThat(recommendationService.findRecommendations(10)).extracting(e -> e.work().getDetails().getTitle()).containsExactly(
        "Terminator",
        "Back to the Future"
      );
    }
  }
}
