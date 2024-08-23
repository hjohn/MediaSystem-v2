package hs.mediasystem.db.services;

import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.base.DatabaseContentPrintProvider;
import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.base.StreamStateStore;
import hs.mediasystem.db.core.DiscoveryController;
import hs.mediasystem.db.core.IdentificationStore;
import hs.mediasystem.db.core.ImportSource;
import hs.mediasystem.db.core.domain.ContentPrint;
import hs.mediasystem.db.core.domain.StreamTags;
import hs.mediasystem.db.extract.StreamDescriptorService;
import hs.mediasystem.db.util.InjectorExtension;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.Attributes;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
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
import static org.mockito.Mockito.when;

@ExtendWith(InjectorExtension.class)
public class RecommendationServiceIT {
  @Produces private static final StreamStateStore STREAM_STATE_STORE = mock(StreamStateStore.class);
  @Produces private static final IdentificationStore IDENTIFICATION_STORE = mock(IdentificationStore.class);
  @Produces private static final StreamDescriptorService STREAM_DESCRIPTOR_SERVICE = mock(StreamDescriptorService.class);
  @Produces private static final DatabaseContentPrintProvider CONTENT_PRINT_PROVIDER = mock(DatabaseContentPrintProvider.class);
  @Produces private static final Collection<ImportSource> IMPORT_SOURCES = List.of();

  private static final Instant INSTANT = Instant.ofEpochMilli(0);

  @Inject private DiscoveryController discoveryController;
  @Inject private RecommendationService recommendationService;
  @Inject private StreamStateService streamStateService;

  private final ImportSource importSource = new ImportSource(
    (root, registry) -> {},
    URI.create("/"),
    Optional.of("TMDB"),
    new StreamTags(Set.of())
    );


  @BeforeEach
  void beforeEach() throws IOException {
    ContentPrint CP1 = new ContentPrint(new ContentID(1), 800L, INSTANT, new byte[16], INSTANT);
    ContentPrint CP2 = new ContentPrint(new ContentID(2), 1954L, INSTANT, new byte[16], INSTANT);

    when(CONTENT_PRINT_PROVIDER.get(URI.create("/Terminator.avi"), 800L, INSTANT)).thenReturn(CP1);
    when(CONTENT_PRINT_PROVIDER.get(URI.create("/BTTF.avi"), 1954L, INSTANT)).thenReturn(CP2);

    discoveryController.registerDiscovery(
      importSource,
      URI.create("/"),
      List.of(
        new Discovery(
          MediaType.MOVIE,
          URI.create("/Terminator.avi"),
          Attributes.of(Attribute.TITLE, "Terminator"),
          INSTANT,
          800L
        ),
        new Discovery(
          MediaType.MOVIE,
          URI.create("/BTTF.avi"),
          Attributes.of(Attribute.TITLE, "Back to the Future"),
          INSTANT,
          1954L
        )
      )
    );
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
