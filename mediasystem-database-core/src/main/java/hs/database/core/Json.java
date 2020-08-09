package hs.database.core;

/**
 * Wrapper for JSON to distinguish regular strings from JSON strings.
 */
public class Json {
  private final String json;

  public Json(String json) {
    this.json = json;
  }

  @Override
  public String toString() {
    return json;
  }
}
