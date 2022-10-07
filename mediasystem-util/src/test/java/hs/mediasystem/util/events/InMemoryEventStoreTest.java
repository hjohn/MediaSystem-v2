package hs.mediasystem.util.events;

public class InMemoryEventStoreTest extends AbstractEventStoreTest {

  protected InMemoryEventStoreTest() {
    super(new InMemoryEventStore<>(), 0);
  }
}
