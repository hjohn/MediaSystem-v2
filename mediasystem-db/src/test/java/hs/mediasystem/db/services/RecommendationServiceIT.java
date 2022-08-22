package hs.mediasystem.db.services;

import hs.ddif.annotations.Produces;
import hs.mediasystem.db.InjectorExtension;
import hs.mediasystem.db.base.DatabaseStreamStoreShim;
import hs.mediasystem.db.base.ImportSource;
import hs.mediasystem.db.base.ImportSourceProvider;
import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.extract.DefaultStreamMetaDataStore;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.skipscan.db.DatabaseConfig;
import hs.mediasystem.util.Attributes;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(InjectorExtension.class)
public class RecommendationServiceIT extends DatabaseConfig {
  @Produces private static final ImportSourceProvider IMPORT_SOURCE_PROVIDER = mock(ImportSourceProvider.class);

  @Inject private ContentPrintProvider contentPrintProvider;
  @Inject private RecommendationService recommendationService;
  @Inject private StreamStateService streamStateService;
  @Inject private DatabaseStreamStoreShim databaseStreamStore;

  @Inject DefaultStreamMetaDataStore streamMetaDataStore;  // cannot be auto-discovered as not directly used, ensure it is present

  @Nested
  class When_findRecommendations_IsCalled {
    {
      ContentID contentId = randomContentId();
      ContentID contentId2 = randomContentId();
      Instant now = Instant.now();

      when(IMPORT_SOURCE_PROVIDER.getImportSource(anyInt())).thenReturn(new ImportSource(null, 1, null, new StreamSource(new StreamTags(Set.of("cartoon")), List.of())));

      databaseStreamStore.put(streamable(MediaType.MOVIE, "uri1", new StreamID(1, contentId, "?"), "Terminator"));
      streamStateService.setLastWatchedTime(contentId, now.minus(2, ChronoUnit.MINUTES));
      streamStateService.setResumePosition(contentId, 60);

      databaseStreamStore.put(streamable(MediaType.MOVIE, "uri2", new StreamID(1, contentId2, "?"), "Back to the Future"));
      streamStateService.setLastWatchedTime(contentId2, now.minus(2, ChronoUnit.DAYS));
      streamStateService.setResumePosition(contentId2, 60);
    }

    @Test
    void shouldReturnExpectedResults() {
      assertThat(recommendationService.findRecommendations(10)).extracting(e -> e.work().getDetails().getTitle()).containsExactly(
        "Terminator",
        "Back to the Future"
      );
    }
  }

  private static Streamable streamable(MediaType type, String uri, StreamID sid, String title) {
    return new Streamable(type, URI.create(uri), sid, null, Attributes.of(Attribute.TITLE, title));
  }

  private ContentID randomContentId() {
    try {
      Path tempFile = Files.createTempFile(null, null);
      ContentPrint contentPrint = contentPrintProvider.get(tempFile.toUri(), (long)(Math.random() * 1000000L), 200L);

      return contentPrint.getId();
    }
    catch(Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
