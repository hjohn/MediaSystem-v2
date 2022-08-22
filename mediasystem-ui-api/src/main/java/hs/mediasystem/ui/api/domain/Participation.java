package hs.mediasystem.ui.api.domain;

public record Participation(Role role, Work work, int episodeCount, double popularity) {
}
