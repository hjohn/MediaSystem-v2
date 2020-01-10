package hs.mediasystem.local.client.service;

import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ui.api.domain.State;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Var;

@Singleton
public class StateFactory {
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