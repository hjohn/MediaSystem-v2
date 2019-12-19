package hs.mediasystem.runner.collection;

public class CollectionDefinition {
  private final String type;
  private final String tag;
  private final String title;

  public CollectionDefinition(String type, String tag, String title) {
    this.type = type;
    this.tag = tag;
    this.title = title;
  }

  public String getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getTag() {
    return tag;
  }
}