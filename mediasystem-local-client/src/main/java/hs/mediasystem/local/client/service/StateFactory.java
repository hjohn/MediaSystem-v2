package hs.mediasystem.local.client.service;

import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.ui.api.domain.State;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Var;

@Singleton
public class StateFactory {
  @Inject private StreamStateService streamStateService;  // TODO this still refers directly to the back-end

  public State create(ContentID contentId) {
    if(contentId == null) {
      return new State(Var.newSimpleVar(null), Var.newSimpleVar(false), Var.newSimpleVar(Duration.ZERO));
    }

    return new State(
      Var.suspendable(streamStateService.lastWatchedTimeProperty(contentId)),
      Var.suspendable(streamStateService.watchedProperty(contentId)),
      Var.suspendable(streamStateService.resumePositionProperty(contentId)).mapBidirectional(i -> Duration.ofSeconds(i), d -> (int)d.toSeconds())
    );
  }
}