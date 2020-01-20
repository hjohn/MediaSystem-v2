package hs.mediasystem.util.parser;

public class Token {
  private final Type type;
  private final String text;
  private final int offset;
  private final String seperator;

  public Token(Type type, String text, String seperator, int offset) {
    this.type = type;
    this.text = text;
    this.seperator = seperator;
    this.offset = offset;
  }

  public String getText() {
    return text;
  }

  public String getSeperator() {
    return seperator;
  }

  public int getOffset() {
    return offset;
  }

  public int getEndOffset() {
    return offset + (text == null ? 0 : text.length());
  }

  public boolean isEnd() {
    return type == null;
  }

  public boolean matches(Type type, String value) {
    return this.type == type && value.equals(text);
  }

  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    return text == null ? "END_OF_INPUT" : "\"" + text + "\"";
  }
}