package hs.mediasystem.runner;

import hs.mediasystem.util.Exceptional;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.WeakHashMap;

import javafx.scene.image.Image;

public class ResourceManager {
  private static final Map<Class<?>, ResourceBundle> DATA = new WeakHashMap<>();

  private final Class<?> cls;

  public ResourceManager(Class<?> cls) {
    this.cls = cls;
  }

  public String getText(String key, String subKey) {
    return getText(cls, key, subKey);
  }

  public String getText(String key) {
    return getText(cls, key);
  }

  public static String getText(Class<?> cls, String key, String subKey) {
    return getText(cls, key + "." + subKey);
  }

  public static String getText(Class<?> cls, String key) {
    return getResourceBundle(cls)
      .map(rb -> rb.getString(key))
      .ignore(MissingResourceException.class)
      .orElse("<" + key + ">");
  }

  public static Image getImage(Class<?> cls, String key) {
    return new Image(cls.getResourceAsStream(cls.getSimpleName() + "_" + key + ".png"));
  }

  public static double getDouble(Class<?> cls, String key) {
    return getResourceBundle(cls).map(rb -> rb.getString(key)).map(Double::parseDouble).orElse(Double.NaN);
  }

  private static Exceptional<ResourceBundle> getResourceBundle(Class<?> cls) {
    return Exceptional.from(() -> DATA.computeIfAbsent(cls, k -> ResourceBundle.getBundle(cls.getName(), Locale.getDefault(), cls.getClassLoader())));
  }
}
