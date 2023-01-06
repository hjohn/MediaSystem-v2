package hs.mediasystem.util.events.streams;

public interface Subscription {

  /**
   * Blocks this thread until the subscription has processed the latest events.
   * Throws {@link IllegalStateException} if the subscription is no longer active.
   *
   * <p>Note that if the subscriber does not control its source, new events may
   * be published at any time and so the subscription can be behind immediately
   * after this call returns.
   *
   * <p>If the source can be blocked, then calling this function will guarantee
   * that all events have been processed by the subscription when the call
   * completes.
   *
   * @throws IllegalStateException when the subscription is no longer active
   */
  void join();

  void unsubscribe();
}