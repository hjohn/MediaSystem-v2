package hs.mediasystem.domain.stream;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamID {
  private static final Pattern PATTERN = Pattern.compile("([-0-9]+):([0-9]+):(.*)");

  private final int importSourceId;
  private final ContentID contentId;
  private final String name;

  public static StreamID of(String encoded) {
    Matcher matcher = PATTERN.matcher(encoded);

    if(!matcher.matches()) {
      throw new IllegalArgumentException("Invalid encoded stream id: " + encoded);
    }

    return new StreamID(Integer.parseInt(matcher.group(1)), new ContentID(Integer.parseInt(matcher.group(2))), matcher.group(3));
  }

  public StreamID(int importSourceId, ContentID contentId, String name) {
    if(contentId == null) {
      throw new IllegalArgumentException("contentId cannot be null");
    }
    if(name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty: " + name);
    }

    this.importSourceId = importSourceId;
    this.contentId = contentId;
    this.name = name;
  }

  public int getImportSourceId() {
    return importSourceId;
  }

  public ContentID getContentId() {
    return contentId;
  }

  public String getName() {
    return name;
  }

  public String asString() {
    return importSourceId + ":" + contentId.asInt() + ":" + name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentId, importSourceId, name);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    StreamID other = (StreamID)obj;

    return Objects.equals(contentId, other.contentId)
        && importSourceId == other.importSourceId
        && Objects.equals(name, other.name);
  }

  @Override
  public String toString() {
    return "StreamID[" + importSourceId + ":" + contentId + ":" + name + "]";
  }
}
