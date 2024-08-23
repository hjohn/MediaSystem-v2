package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.base.StreamStateStore;
import hs.mediasystem.db.core.IdentificationStore;
import hs.mediasystem.db.core.StreamableEvent;
import hs.mediasystem.db.core.domain.ContentPrint;
import hs.mediasystem.db.core.domain.StreamTags;
import hs.mediasystem.db.core.domain.Streamable;
import hs.mediasystem.db.extract.StreamDescriptorService;
import hs.mediasystem.db.util.InjectorExtension;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.events.InMemoryEventStore;
import hs.mediasystem.util.events.SimpleEventStream;
import hs.mediasystem.util.events.streams.EventStream;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
  @Produces private static final StreamStateStore STREAM_STATE_STORE = mock(StreamStateStore.class);
  @Produces private static final EventStream<StreamableEvent> STREAMABLE_EVENTS = new SimpleEventStream<>(new InMemoryEventStore<>(StreamableEvent.class));
  @Produces private static final IdentificationStore IDENTIFICATION_STORE = mock(IdentificationStore.class);
  @Produces private static final StreamDescriptorService STREAM_DESCRIPTOR_SERVICE = mock(StreamDescriptorService.class);

  @Inject private RecommendationService recommendationService;
  @Inject private StreamStateService streamStateService;

  @BeforeEach
  void beforeEach() {
    ContentPrint CP1 = new ContentPrint(new ContentID(1), 12345L, Instant.now(), new byte[16], Instant.now());
    ContentPrint CP2 = new ContentPrint(new ContentID(2), 12345L, Instant.now(), new byte[16], Instant.now());

    STREAMABLE_EVENTS.push(new StreamableEvent.Updated(
      new Streamable(
        MediaType.MOVIE,
        Path.of("/Terminator.avi").toUri(),
        CP1,
        Optional.empty(),
        new StreamTags(Set.of("cartoons")),
        Optional.empty()
      ),
      Optional.of(mock(IdentificationProvider.class)),
      new Discovery(
        MediaType.MOVIE,
        Path.of("/Terminator.avi").toUri(),
        Attributes.of(Attribute.TITLE, "Terminator"),
        Instant.now(),
        123L
      )
    ));

    STREAMABLE_EVENTS.push(new StreamableEvent.Updated(
      new Streamable(
        MediaType.MOVIE,
        Path.of("/BTTF.avi").toUri(),
        CP2,
        Optional.empty(),
        new StreamTags(Set.of("cartoons")),
        Optional.empty()
      ),
      Optional.of(mock(IdentificationProvider.class)),
      new Discovery(
        MediaType.MOVIE,
        Path.of("/BTTF.avi").toUri(),
        Attributes.of(Attribute.TITLE, "Back to the Future"),
        Instant.now(),
        124L
      )
    ));
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
