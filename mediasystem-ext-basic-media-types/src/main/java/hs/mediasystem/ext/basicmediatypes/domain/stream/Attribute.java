package hs.mediasystem.ext.basicmediatypes.domain.stream;

public interface Attribute {
  public static final String TITLE = "title";
  public static final String ALTERNATIVE_TITLE = "alternativeTitle";
  public static final String SUBTITLE = "subtitle";
  public static final String SEQUENCE = "sequence";
  public static final String YEAR = "year";
  public static final String ID_PREFIX = "id:";
  public static final String CHILD_TYPE = "childType";

  public enum ChildType {
    EPISODE,
    SPECIAL,
    EXTRA
  }
}
