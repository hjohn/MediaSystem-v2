package hs.mediasystem.domain.work;

/**
 * Represents a link to a video.
 *
 * @param type the {@link Type}, can be {@code null} if unknown
 * @param name a name, cannot be {@code null}
 * @param site a site, cannot be {@code null}
 * @param key a key, cannot be {@code null}
 * @param size the size, always positive
 */
public record VideoLink(Type type, String name, String site, String key, int size) {
  public enum Type {TRAILER, CLIP, TEASER, FEATURETTE}

  public VideoLink {
    if(name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if(site == null) {
      throw new IllegalArgumentException("site cannot be null");
    }
    if(key == null) {
      throw new IllegalArgumentException("key cannot be null");
    }
    if(size <= 0) {
      throw new IllegalArgumentException("size must be positive");
    }
  }
}
