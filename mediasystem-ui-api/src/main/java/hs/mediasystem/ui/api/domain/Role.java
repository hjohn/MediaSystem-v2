package hs.mediasystem.ui.api.domain;

public record Role(Type type, String character, String job) {
  public enum Type {CAST, CREW, GUEST_STAR}
}
