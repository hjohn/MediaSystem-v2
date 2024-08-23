package hs.mediasystem.db.core;

import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.base.DatabaseContentPrintProvider;
import hs.mediasystem.db.core.domain.ContentPrint;
import hs.mediasystem.db.core.domain.StreamTags;
import hs.mediasystem.db.core.domain.Streamable;
import hs.mediasystem.db.extract.StreamDescriptorService;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.Attributes;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StreamableServiceTest {
  private static final Attributes ATTRIBUTES = Attributes.of("title", "title");
  private static final Optional<IdentificationProvider> ID_SERVICE = Optional.of(mock(IdentificationProvider.class));
  private static final StreamTags TAGS = new StreamTags(Set.of("cartoon"));

  private final Deque<StreamableEvent> queue = new ArrayDeque<>();

  @Mock private StreamDescriptorService streamDescriptorService;
  @Mock private DatabaseContentPrintProvider contentPrintProvider;
  @Mock private ResourceService resourceService;

  private StreamableService service;

  @BeforeEach
  void beforeEach() throws IOException {
    when(contentPrintProvider.get(any(), any(), any())).thenAnswer(x -> contentPrint(((Instant)x.getArgument(2)).toEpochMilli()));

    service = new StreamableService(contentPrintProvider, streamDescriptorService, resourceService);

    doAnswer(x -> queue.add(x.getArgument(0))).when(resourceService).handleEvent(any());
  }

  @Test
  public void shouldProduceExpectedDiffEvents() {
    URI root = Path.of("/").toUri().resolve("");

    Discovery aParent = serieDiscovery(root.resolve("a"), 1000);
    Discovery a1Parent = serieDiscovery(root.resolve("a/1"), 1001);
    Discovery bParent = serieDiscovery(root.resolve("b"), 1002);
    Discovery cParent = serieDiscovery(root.resolve("c"), 1003);

    service.push(new DiscoverEvent(root, ID_SERVICE, TAGS, Optional.empty(), List.of(
      serieDiscovery(root.resolve("b"), 1),
      serieDiscovery(root.resolve("d"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updatedSerie(root.resolve("b"), 1),
        updatedSerie(root.resolve("d"), 1)
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(bParent.location(), ID_SERVICE, TAGS, Optional.of(bParent.location()), List.of(
      discovery(root.resolve("b/1"), 1),
      discovery(root.resolve("b/2"), 1),
      discovery(root.resolve("b/3"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("b/1"), bParent, 1),
        updated(root.resolve("b/2"), bParent, 1),
        updated(root.resolve("b/3"), bParent, 1)
      )
    );

    queue.clear();

    // Re-discovery of b and d should do nothing:
    service.push(new DiscoverEvent(root, ID_SERVICE, TAGS, Optional.empty(), List.of(
      serieDiscovery(root.resolve("b"), 1),
      serieDiscovery(root.resolve("d"), 1)
    )));

//    subscription.join();

    assertThat(queue).isEmpty();

    queue.clear();

    service.push(new DiscoverEvent(aParent.location(), ID_SERVICE, TAGS, Optional.of(aParent.location()), List.of(
      discovery(root.resolve("a/1"), 1),
      discovery(root.resolve("a/2"), 1),
      discovery(root.resolve("a/3"), 1)
    )));

//    subscription.join();

    assertThat(queue).isEmpty();  // aParent wasn't registered yet, so these sub items are ignored

    queue.clear();

    service.push(new DiscoverEvent(cParent.location(), ID_SERVICE, TAGS, Optional.of(cParent.location()), List.of(
      discovery(root.resolve("c/1"), 1)
    )));

//    subscription.join();

    assertThat(queue).isEmpty();  // cParent wasn't registered yet, so these sub items are ignored

    queue.clear();

    service.push(new DiscoverEvent(root, ID_SERVICE, TAGS, Optional.empty(), List.of(
      serieDiscovery(root.resolve("a"), 1),
      serieDiscovery(root.resolve("b"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updatedSerie(root.resolve("a"), 1),
        new StreamableEvent.Removed(root.resolve("d"))
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(aParent.location(), ID_SERVICE, TAGS, Optional.of(aParent.location()), List.of(
      discovery(root.resolve("a/1"), 1),
      discovery(root.resolve("a/2"), 1),
      discovery(root.resolve("a/3"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/1"), aParent, 1),
        updated(root.resolve("a/2"), aParent, 1),
        updated(root.resolve("a/3"), aParent, 1)
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(bParent.location(), ID_SERVICE, TAGS, Optional.of(bParent.location()), List.of(
      discovery(root.resolve("b/1"), 1),
      discovery(root.resolve("b/2"), 2),
      discovery(root.resolve("b/3"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("b/2"), bParent, 2)
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(aParent.location(), ID_SERVICE, TAGS, Optional.of(aParent.location()), List.of(
      discovery(root.resolve("a/4"), 1),
      discovery(root.resolve("a/1"), 1),
      discovery(root.resolve("a/3"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/4"), aParent, 1),
        new StreamableEvent.Removed(root.resolve("a/2"))
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(aParent.location(), ID_SERVICE, TAGS, Optional.of(aParent.location()), List.of(
      discovery(root.resolve("a/1"), 1),
      discovery(root.resolve("a/2"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/2"), aParent, 1),
        new StreamableEvent.Removed(root.resolve("a/3")),
        new StreamableEvent.Removed(root.resolve("a/4"))
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(a1Parent.location(), ID_SERVICE, TAGS, Optional.of(a1Parent.location()), List.of(
      discovery(root.resolve("a/1/A"), 1),
      discovery(root.resolve("a/1/B"), 1),
      discovery(root.resolve("a/1/C"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/1/A"), a1Parent, 1),
        updated(root.resolve("a/1/B"), a1Parent, 1),
        updated(root.resolve("a/1/C"), a1Parent, 1)
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(aParent.location(), ID_SERVICE, TAGS, Optional.of(aParent.location()), List.of(
      discovery(root.resolve("a/2"), 1)
    )));

    // TODO removal of parent should happen after children?
    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        new StreamableEvent.Removed(root.resolve("a/1")),
        new StreamableEvent.Removed(root.resolve("a/1/A")),
        new StreamableEvent.Removed(root.resolve("a/1/B")),
        new StreamableEvent.Removed(root.resolve("a/1/C"))
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(root, ID_SERVICE, TAGS, Optional.empty(), List.of()));  // empty, everything gone

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        new StreamableEvent.Removed(root.resolve("a")),
        new StreamableEvent.Removed(root.resolve("a/2")),
        new StreamableEvent.Removed(root.resolve("b")),
        new StreamableEvent.Removed(root.resolve("b/1")),
        new StreamableEvent.Removed(root.resolve("b/2")),
        new StreamableEvent.Removed(root.resolve("b/3"))
      )
    );
  }

  @Test
  void shouldLoadPreviousEventsBeforeDoingNewDiffs() {
    URI root = Path.of("/").toUri();
    Discovery aParent = serieDiscovery(root.resolve("a"), 1000);

    service.push(new DiscoverEvent(root, ID_SERVICE, TAGS, Optional.empty(), List.of(
      serieDiscovery(root.resolve("a"), 1)
    )));

    service.push(new DiscoverEvent(aParent.location(), ID_SERVICE, TAGS, Optional.of(aParent.location()), List.of(
      discovery(root.resolve("a/1"), 1),
      discovery(root.resolve("a/2"), 1),
      discovery(root.resolve("a/3"), 1)
    )));

    queue.clear();

    service.push(new DiscoverEvent(aParent.location(), ID_SERVICE, TAGS, Optional.of(aParent.location()), List.of(
      discovery(root.resolve("a/1"), 2),
      discovery(root.resolve("a/3"), 1),
      discovery(root.resolve("a/4"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/1"), aParent, 2),
        updated(root.resolve("a/4"), aParent, 1),
        new StreamableEvent.Removed(root.resolve("a/2"))
      )
    );
  }

  @Test
  void shouldNotGetConfusedWhenABaseURLIsAPrefixOfAnother() {
    URI root = Path.of("/").toUri();
    Discovery aParent = serieDiscovery(root.resolve("a"), 1000);
    Discovery aPostfixParent = serieDiscovery(root.resolve("a%20postfix"), 1001);

    service.push(new DiscoverEvent(root, ID_SERVICE, TAGS, Optional.empty(), List.of(
      serieDiscovery(root.resolve("a"), 1),
      serieDiscovery(root.resolve("a%20postfix"), 1)
    )));

    service.push(new DiscoverEvent(aParent.location(), ID_SERVICE, TAGS, Optional.of(aParent.location()), List.of(
      discovery(root.resolve("a/1"), 1),
      discovery(root.resolve("a/2"), 1),
      discovery(root.resolve("a/3"), 1)
    )));

    service.push(new DiscoverEvent(aPostfixParent.location(), ID_SERVICE, TAGS, Optional.of(aPostfixParent.location()), List.of(
      discovery(root.resolve("a%20postfix/4"), 1),
      discovery(root.resolve("a%20postfix/5"), 1),
      discovery(root.resolve("a%20postfix/6"), 1)
    )));

    queue.clear();

    service.push(new DiscoverEvent(aParent.location(), ID_SERVICE, TAGS, Optional.of(aParent.location()), List.of(
      discovery(root.resolve("a/1"), 2),
      discovery(root.resolve("a/3"), 1),
      discovery(root.resolve("a/4"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/1"), aParent, 2),
        updated(root.resolve("a/4"), aParent, 1),
        new StreamableEvent.Removed(root.resolve("a/2"))
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(aPostfixParent.location(), ID_SERVICE, TAGS, Optional.of(aPostfixParent.location()), List.of(
      discovery(root.resolve("a%20postfix/4"), 1),
      discovery(root.resolve("a%20postfix/5"), 1),
      discovery(root.resolve("a%20postfix/6"), 1),
      discovery(root.resolve("a%20postfix/7"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a%20postfix/7"), aPostfixParent, 1)
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(root, ID_SERVICE, TAGS, Optional.empty(), List.of(
      serieDiscovery(root.resolve("a"), 1),
      serieDiscovery(root.resolve("a%20postfix"), 1),
      serieDiscovery(root.resolve("b"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updatedSerie(root.resolve("b"), 1)
      )
    );
  }

  @Test
  void shouldReactWhenHigherPathDoesNotMentionExistingPaths() {
    URI realRoot = Path.of("/").toUri().resolve("/");
    URI subRoot = Path.of("/").toUri().resolve("/abc/def/");

    service.push(new DiscoverEvent(subRoot, ID_SERVICE, TAGS, Optional.empty(), List.of(
      serieDiscovery(subRoot.resolve("b"), 1),
      serieDiscovery(subRoot.resolve("d"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updatedSerie(subRoot.resolve("b"), 1),
        updatedSerie(subRoot.resolve("d"), 1)
      )
    );

    queue.clear();

    service.push(new DiscoverEvent(realRoot, ID_SERVICE, TAGS, Optional.empty(), List.of(
      serieDiscovery(realRoot.resolve("xyz"), 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        new StreamableEvent.Removed(realRoot.resolve("/abc/def/b")),
        new StreamableEvent.Removed(realRoot.resolve("/abc/def/d")),
        updatedSerie(realRoot.resolve("xyz"), 1)
      )
    );
  }

  private static ContentPrint contentPrint(long time) {
    return new ContentPrint(new ContentID(1), null, Instant.ofEpochMilli(time), new byte[] {1, 2, 3}, Instant.ofEpochSecond(1000));
  }

  // time here is used to trigger a difference other than in the path
  private static Discovery discovery(URI path, long time) {
    return new Discovery(MediaType.EPISODE, path, ATTRIBUTES, Instant.ofEpochMilli(time), 20000L);
  }

  private static Discovery serieDiscovery(URI path, long time) {
    return new Discovery(MediaType.SERIE, path, ATTRIBUTES, Instant.ofEpochMilli(time), null);
  }

  private static StreamableEvent.Updated updatedSerie(URI path, long time) {
    return new StreamableEvent.Updated(new Streamable(MediaType.SERIE, path, contentPrint(time), Optional.empty(), TAGS, Optional.empty()), ID_SERVICE, new Discovery(MediaType.SERIE, path, ATTRIBUTES, Instant.ofEpochMilli(time), null));
  }

  private static StreamableEvent.Updated updated(URI path, Discovery parent, long time) {
    return new StreamableEvent.Updated(new Streamable(MediaType.EPISODE, path, contentPrint(time), Optional.of(parent.location()), TAGS, Optional.empty()), ID_SERVICE, new Discovery(MediaType.EPISODE, path, ATTRIBUTES, Instant.ofEpochMilli(time), 20000L));
  }
}
