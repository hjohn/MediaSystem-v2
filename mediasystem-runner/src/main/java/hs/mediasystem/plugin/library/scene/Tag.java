package hs.mediasystem.plugin.library.scene;

public interface Tag<T> {
  public static Tag<Boolean> asBoolean(String name) {
    return new Tag<>() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Boolean fromString(String text) {
        return text == null ? null : Boolean.valueOf(text);
      }

      @Override
      public String toString(Boolean value) {
        return value == null ? null : Boolean.toString(value);
      }
    };
  }

  String getName();
  T fromString(String text);
  String toString(T value);
}
