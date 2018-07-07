package hs.mediasystem.ext.basicmediatypes;

public class VideoLink {
  public enum Type {TRAILER, CLIP, TEASER, FEATURETTE}

  private final Type type;
  private final String name;
  private final String site;
  private final String key;
  private final int size;

  public VideoLink(Type type, String name, String site, String key, int size) {
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

    this.type = type;
    this.name = name;
    this.site = site;
    this.key = key;
    this.size = size;
  }

  public String getName() {
    return name;
  }

  /**
   * Returns the type.
   *
   * @return the type, can be null if unknown
   */
  public Type getType() {
    return type;
  }

  public String getSite() {
    return site;
  }

  public String getKey() {
    return key;
  }

  public Integer getSize() {
    return size;
  }
}
