package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.domain.Identification;
import hs.mediasystem.api.datasource.domain.Release;
import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.services.IdentificationTaskManager.IdentifiedLocation;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.time.SimulatedTimeSource;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdentificationTaskManagerTest {
  @Mock private IdentificationProvider identificationProvider;
  @Mock private IdentificationStore identificationStore;

  private IdentificationTaskManager manager;

  private static final Release RELEASE = mock(Release.class);
  private static final Release RELEASE2 = mock(Release.class);
  private static final Attributes ATTRIBUTES = Attributes.of(Attribute.TITLE, "title");
  private static final Discovery D1 = new Discovery(MediaType.SERIE, URI.create("charmed"), ATTRIBUTES, Instant.now(), 20000L);
  private static final Instant BASE = Instant.ofEpochSecond(0);

  private final BlockingQueue<IdentifiedLocation> identifications = new ArrayBlockingQueue<>(50);
  private final Match match = mock(Match.class);
  private final Match match2 = mock(Match.class);
  private final Identification identification = new Identification(List.of(RELEASE), match);
  private final Identification identification2 = new Identification(List.of(RELEASE2), match2);
  private final SimulatedTimeSource source = new SimulatedTimeSource(Instant.EPOCH);

  @BeforeEach
  void beforeEach() {
    this.identifications.clear();
    this.manager = new IdentificationTaskManager(
      identificationStore,
      source,
      Duration.ofHours(24),
      Duration.ofHours(2),
      identifications
    );
  }

  @Test
  void shouldImmediatelyIdentifyNewItem() throws IOException, SQLException {
    when(identificationProvider.identify(D1)).thenReturn(Optional.of(identification));
    when(identificationStore.find(D1.location())).thenReturn(Optional.empty());

    manager.create(identificationProvider, D1);

    await().untilAsserted(() ->
      assertThat(identifications).containsExactly(
        new IdentifiedLocation(D1.location(), identification, identificationProvider)
      )
    );
  }

  @Test
  void shouldInitiallyIdentifyItemWithDatabaseThenLaterUpdateIt() throws IOException, SQLException {
    when(identificationStore.find(D1.location())).thenReturn(Optional.of(identification));
    when(match.creationTime()).thenReturn(BASE.minus(Duration.ofHours(5)));  // no need to reidentify for another 24 - 5 hours

    manager.create(identificationProvider, D1);

    await().untilAsserted(() ->
      assertThat(identifications).containsExactly(
        new IdentifiedLocation(D1.location(), identification, identificationProvider)
      )
    );

    // Advance 20 hours:
    when(identificationProvider.identify(D1)).thenReturn(Optional.of(identification2));

    identifications.clear();
    source.advanceTime(Duration.ofHours(20));

    await().untilAsserted(() ->
      assertThat(identifications).containsExactly(
        new IdentifiedLocation(D1.location(), identification2, identificationProvider)
      )
    );

    verify(identificationStore).store(D1.location(), identification2);

    // Advance 25 hours:
    when(identificationProvider.identify(D1)).thenReturn(Optional.of(identification));

    identifications.clear();
    source.advanceTime(Duration.ofHours(25));

    await().untilAsserted(() ->
      assertThat(identifications).containsExactly(
        new IdentifiedLocation(D1.location(), identification, identificationProvider)
      )
    );

    verify(identificationStore).store(D1.location(), identification);

    // Advance 25 hours:
    when(identificationProvider.identify(D1)).thenReturn(Optional.of(identification));

    identifications.clear();
    source.advanceTime(Duration.ofHours(25));

    await().during(Duration.ofSeconds(2)).untilAsserted(() ->
      assertThat(identifications).isEmpty()  // ensure callback not called again for identical resultcontainsExactly(
    );
  }

  @Test
  void shouldSendOutEmptyIdentificationWhenIdentificationNoLongerProducedMatch() throws SQLException, IOException {
    when(identificationStore.find(D1.location())).thenReturn(Optional.of(identification));
    when(match.creationTime()).thenReturn(BASE.minus(Duration.ofHours(5)));  // no need to reidentify for another 24 - 5 hours

    manager.create(identificationProvider, D1);

    await().untilAsserted(() ->
      assertThat(identifications).containsExactly(
        new IdentifiedLocation(D1.location(), identification, identificationProvider)
      )
    );

    verify(identificationStore, never()).store(any(), any());

    // Advance 25 hours:
    when(identificationProvider.identify(D1)).thenReturn(Optional.empty());

    identifications.clear();
    source.advanceTime(Duration.ofHours(25));

    await().untilAsserted(() ->
      assertThat(identifications).containsExactly(
        new IdentifiedLocation(D1.location(), null, identificationProvider)
      )
    );

    // Advance 25 hours:
    when(identificationProvider.identify(D1)).thenReturn(Optional.empty());

    identifications.clear();
    source.advanceTime(Duration.ofHours(25));

    await().during(Duration.ofSeconds(2)).untilAsserted(() ->
      assertThat(identifications).isEmpty()  // ensure callback not called again for identical result
    );
  }

  @Test
  void shouldRescheduleIOExceptionsFaster() throws IOException {
    when(identificationProvider.identify(D1)).thenThrow(IOException.class);

    manager.create(identificationProvider, D1);

    await().during(1, TimeUnit.SECONDS).untilAsserted(() -> assertThat(identifications.isEmpty()));

    // Now verify that an identification happens after just 3 hours:
    reset(identificationProvider);
    when(identificationProvider.identify(D1)).thenReturn(Optional.of(identification));

    source.advanceTime(Duration.ofHours(3));

    await().untilAsserted(() ->
      assertThat(identifications).containsExactly(
        new IdentifiedLocation(D1.location(), identification, identificationProvider)
      )
    );
  }

  @Test
  void shouldRescheduleOtherExceptionsFaster() throws IOException {
    when(identificationProvider.identify(D1)).thenThrow(IllegalArgumentException.class);

    manager.create(identificationProvider, D1);

    await().during(1, TimeUnit.SECONDS).untilAsserted(() -> assertThat(identifications.isEmpty()));

    // Now verify that an identification happens after just 3 hours:
    reset(identificationProvider);
    when(identificationProvider.identify(D1)).thenReturn(Optional.of(identification));

    source.advanceTime(Duration.ofHours(3));

    await().untilAsserted(() ->
      assertThat(identifications).containsExactly(
        new IdentifiedLocation(D1.location(), identification, identificationProvider)
      )
    );
  }

  @Test
  void shouldStopExistingItem() throws IOException {
    when(identificationProvider.identify(D1)).thenReturn(Optional.of(identification));

    manager.create(identificationProvider, D1);

    await().untilAsserted(() ->
      assertThat(identifications).containsExactly(
        new IdentifiedLocation(D1.location(), identification, identificationProvider)
      )
    );

    // Untrack and assert that no new identifications occur (due to faulty interrupt handling for example)
    identifications.clear();

    manager.stop(D1.location());

    await().during(1, TimeUnit.SECONDS).untilAsserted(() ->
      assertThat(identifications).isEmpty()
    );

    // Skip ahead 25 hours to ensure thread is really gone and no new identification occurs:
    identifications.clear();
    source.advanceTime(Duration.ofHours(25));

    await().during(1, TimeUnit.SECONDS).untilAsserted(() ->
      assertThat(identifications).isEmpty()
    );
  }
}
