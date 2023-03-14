package hs.mediasystem.db.core;

import hs.mediasystem.api.discovery.ContentPrint;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.events.InMemoryEventStore;
import hs.mediasystem.util.events.SynchronousEventStream;
import hs.mediasystem.util.events.streams.EventStream;
import hs.mediasystem.util.events.streams.Subscription;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
public class StreamableServiceTest {
  private static final Attributes ATTRIBUTES = Attributes.of("title", "title");
  private static final Optional<String> ID_SERVICE = Optional.of("TMDB");
  private static final StreamTags TAGS = new StreamTags(Set.of("cartoon"));

  private final EventStream<DiscoverEvent> discoverEvents = new SynchronousEventStream<>();
  private final InMemoryEventStore<StreamableEvent> store = new InMemoryEventStore<>(StreamableEvent.class);
  private final Deque<StreamableEvent> queue = new ArrayDeque<>();

  @Test
  public void shouldProduceExpectedDiffEvents() {
    StreamableService differ = new StreamableService(store, discoverEvents);

    URI root = Path.of("/").toUri().resolve("");

    URI aParent = root.resolve("a");
    URI a1Parent = root.resolve("a/1");
    URI bParent = root.resolve("b");
    URI cParent = root.resolve("c");

    Subscription subscription = differ.events().plain().subscribe(queue::add);

    discoverEvents.push(new DiscoverEvent(root, ID_SERVICE, TAGS, List.of(
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

    await().untilAsserted(() ->
      discoverEvents.push(new DiscoverEvent(bParent, ID_SERVICE, TAGS, List.of(
        discovery(root.resolve("b/1"), bParent, 1),
        discovery(root.resolve("b/2"), bParent, 1),
        discovery(root.resolve("b/3"), bParent, 1)
      )))
    );

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("b/1"), bParent, 1),
        updated(root.resolve("b/2"), bParent, 1),
        updated(root.resolve("b/3"), bParent, 1)
      )
    );

    queue.clear();

    // Re-discovery of b and d should do nothing:
    discoverEvents.push(new DiscoverEvent(root, ID_SERVICE, TAGS, List.of(
      serieDiscovery(root.resolve("b"), 1),
      serieDiscovery(root.resolve("d"), 1)
    )));

    subscription.join();

    assertThat(queue).isEmpty();

    queue.clear();

    discoverEvents.push(new DiscoverEvent(aParent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("a/1"), aParent, 1),
      discovery(root.resolve("a/2"), aParent, 1),
      discovery(root.resolve("a/3"), aParent, 1)
    )));

    subscription.join();

    assertThat(queue).isEmpty();  // aParent wasn't registered yet, so these sub items are ignored

    queue.clear();

    discoverEvents.push(new DiscoverEvent(cParent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("c/1"), cParent, 1)
    )));

    subscription.join();

    assertThat(queue).isEmpty();  // cParent wasn't registered yet, so these sub items are ignored

    queue.clear();

    discoverEvents.push(new DiscoverEvent(root, ID_SERVICE, TAGS, List.of(
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

    discoverEvents.push(new DiscoverEvent(aParent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("a/1"), aParent, 1),
      discovery(root.resolve("a/2"), aParent, 1),
      discovery(root.resolve("a/3"), aParent, 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/1"), aParent, 1),
        updated(root.resolve("a/2"), aParent, 1),
        updated(root.resolve("a/3"), aParent, 1)
      )
    );

    queue.clear();

    discoverEvents.push(new DiscoverEvent(bParent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("b/1"), bParent, 1),
      discovery(root.resolve("b/2"), bParent, 2),
      discovery(root.resolve("b/3"), bParent, 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("b/2"), bParent, 2)
      )
    );

    queue.clear();

    discoverEvents.push(new DiscoverEvent(aParent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("a/4"), aParent, 1),
      discovery(root.resolve("a/1"), aParent, 1),
      discovery(root.resolve("a/3"), aParent, 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/4"), aParent, 1),
        new StreamableEvent.Removed(root.resolve("a/2"))
      )
    );

    queue.clear();

    discoverEvents.push(new DiscoverEvent(aParent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("a/1"), aParent, 1),
      discovery(root.resolve("a/2"), aParent, 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/2"), aParent, 1),
        new StreamableEvent.Removed(root.resolve("a/3")),
        new StreamableEvent.Removed(root.resolve("a/4"))
      )
    );

    queue.clear();

    discoverEvents.push(new DiscoverEvent(a1Parent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("a/1/A"), a1Parent, 1),
      discovery(root.resolve("a/1/B"), a1Parent, 1),
      discovery(root.resolve("a/1/C"), a1Parent, 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/1/A"), a1Parent, 1),
        updated(root.resolve("a/1/B"), a1Parent, 1),
        updated(root.resolve("a/1/C"), a1Parent, 1)
      )
    );

    queue.clear();

    discoverEvents.push(new DiscoverEvent(aParent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("a/2"), aParent, 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        new StreamableEvent.Removed(root.resolve("a/1")),
        new StreamableEvent.Removed(root.resolve("a/1/A")),
        new StreamableEvent.Removed(root.resolve("a/1/B")),
        new StreamableEvent.Removed(root.resolve("a/1/C"))
      )
    );

    queue.clear();

    discoverEvents.push(new DiscoverEvent(root, ID_SERVICE, TAGS, List.of()));  // empty, everything gone

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
    URI aParent = root.resolve("a");

    store.append(appender -> {
      appender.append(updatedSerie(root.resolve("a"), 1));
      appender.append(updated(root.resolve("a/1"), aParent, 1));
      appender.append(updated(root.resolve("a/2"), aParent, 1));
      appender.append(updated(root.resolve("a/3"), aParent, 1));
    });

    StreamableService differ = new StreamableService(store, discoverEvents);
    Subscription subscription = differ.events().plain().subscribe(queue::add);

    subscription.join();
    queue.clear();

    discoverEvents.push(new DiscoverEvent(aParent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("a/1"), aParent, 2),
      discovery(root.resolve("a/3"), aParent, 1),
      discovery(root.resolve("a/4"), aParent, 1)
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
    URI aParent = root.resolve("a");
    URI aPostfixParent = root.resolve("a%20postfix");

    store.append(appender -> {
      appender.append(updatedSerie(root.resolve("a"), 1));
      appender.append(updated(root.resolve("a/1"), aParent, 1));
      appender.append(updated(root.resolve("a/2"), aParent, 1));
      appender.append(updated(root.resolve("a/3"), aParent, 1));
      appender.append(updatedSerie(root.resolve("a%20postfix"), 1));
      appender.append(updated(root.resolve("a%20postfix/4"), aPostfixParent, 1));
      appender.append(updated(root.resolve("a%20postfix/5"), aPostfixParent, 1));
      appender.append(updated(root.resolve("a%20postfix/6"), aPostfixParent, 1));
    });

    StreamableService differ = new StreamableService(store, discoverEvents);
    Subscription subscription = differ.events().plain().subscribe(queue::add);

    subscription.join();
    queue.clear();

    discoverEvents.push(new DiscoverEvent(aParent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("a/1"), aParent, 2),
      discovery(root.resolve("a/3"), aParent, 1),
      discovery(root.resolve("a/4"), aParent, 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a/1"), aParent, 2),
        updated(root.resolve("a/4"), aParent, 1),
        new StreamableEvent.Removed(root.resolve("a/2"))
      )
    );

    queue.clear();

    discoverEvents.push(new DiscoverEvent(aPostfixParent, ID_SERVICE, TAGS, List.of(
      discovery(root.resolve("a%20postfix/4"), aPostfixParent, 1),
      discovery(root.resolve("a%20postfix/5"), aPostfixParent, 1),
      discovery(root.resolve("a%20postfix/6"), aPostfixParent, 1),
      discovery(root.resolve("a%20postfix/7"), aPostfixParent, 1)
    )));

    await().untilAsserted(() ->
      assertThat(queue).containsExactlyInAnyOrder(
        updated(root.resolve("a%20postfix/7"), aPostfixParent, 1)
      )
    );

    queue.clear();

    discoverEvents.push(new DiscoverEvent(root, ID_SERVICE, TAGS, List.of(
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

  private static ContentPrint contentPrint(int id) {
    return new ContentPrint(new ContentID(id), null, 1000L, new byte[] {1, 2, 3}, Instant.ofEpochSecond(1000));
  }

  private static Discovery discovery(URI path, URI parent, int id) {
    return new Discovery(MediaType.EPISODE, path, ATTRIBUTES, Optional.of(parent), contentPrint(id));
  }

  private static Discovery serieDiscovery(URI path, int id) {
    return new Discovery(MediaType.SERIE, path, ATTRIBUTES, Optional.empty(), contentPrint(id));
  }

  private static StreamableEvent.Updated updatedSerie(URI path, int id) {
    return new StreamableEvent.Updated(new Streamable(new Discovery(MediaType.SERIE, path, ATTRIBUTES, Optional.empty(), contentPrint(id)), ID_SERVICE, TAGS));
  }

  private static StreamableEvent.Updated updated(URI path, URI parent, int id) {
    return new StreamableEvent.Updated(new Streamable(new Discovery(MediaType.EPISODE, path, ATTRIBUTES, Optional.of(parent), contentPrint(id)), ID_SERVICE, TAGS));
  }
}
