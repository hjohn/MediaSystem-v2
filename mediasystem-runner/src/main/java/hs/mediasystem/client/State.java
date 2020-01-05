package hs.mediasystem.client;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.scanner.api.StreamID;

import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Var;

public class State {
  private final Var<Instant> lastConsumptionTime;
  private final Var<Boolean> consumed;
  private final Var<Duration> resumePosition;

  @Singleton
  public static class Factory {
    @Inject private StreamStateService streamStateService;

    public State create(StreamID streamId) {
      if(streamId == null) {
        return new State(Var.newSimpleVar(null), Var.newSimpleVar(false), Var.newSimpleVar(Duration.ZERO));
      }

      return new State(
        Var.suspendable(streamStateService.lastWatchedTimeProperty(streamId)),
        Var.suspendable(streamStateService.watchedProperty(streamId)),
        Var.suspendable(streamStateService.resumePositionProperty(streamId)).mapBidirectional(i -> Duration.ofSeconds(i), d -> (int)d.toSeconds())
      );
    }
  }

  private State(Var<Instant> lastWatchedTime, Var<Boolean> consumed, Var<Duration> resumePosition) {
    this.lastConsumptionTime = lastWatchedTime;
    this.consumed = consumed;
    this.resumePosition = resumePosition;
  }

  public Var<Instant> getLastConsumptionTime() {
    return lastConsumptionTime;
  }

  public Var<Boolean> isConsumed() {
    return consumed;
  }

  public Var<Duration> getResumePosition() {
    return resumePosition;
  }
}
