package hs.mediasystem.ui.api.domain;

public enum Stage {
  PLANNED,
  IN_PRODUCTION,
  RELEASED,       // When first episode is aired, or final stage for movies
  ENDED           // When serie has ended
}
