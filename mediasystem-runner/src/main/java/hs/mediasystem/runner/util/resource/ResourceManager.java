package hs.mediasystem.runner.util.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import hs.mediasystem.util.exception.Exceptional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.WeakHashMap;

import javafx.scene.image.Image;

public class ResourceManager {
  private static final Map<Class<?>, ResourceBundle> DATA = new WeakHashMap<>();
  private static final Map<Class<?>, Map<Object, String>> KEY_MAPPINGS = new WeakHashMap<>();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());
  private static final YamlControl YAML_RESOURCE_BUNDLE_CONTROL = new YamlControl();

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

  public String getText(Object obj) {
    return getText(cls, obj);
  }

  public static void register(Class<?> cls, Object obj, String key) {
    KEY_MAPPINGS.computeIfAbsent(cls, k -> new WeakHashMap<>()).put(obj, key);
  }

  public static <T> T map(Class<?> cls, String key, T obj) {
    register(cls, obj, key);

    return obj;
  }

  public static String getText(Class<?> cls, String key, String subKey) {
    return getText(cls, key + "." + subKey);
  }

  public static String getText(Class<?> cls, Object obj) {
    return getText(cls, KEY_MAPPINGS.getOrDefault(cls, Collections.emptyMap()).getOrDefault(obj, "unmapped"));
  }

  public static String getText(Class<?> cls, String key) {
    return getResourceBundle(cls)
      .map(rb -> rb.getString(key))
      .ignore(MissingResourceException.class)
      .orElse("<" + cls + "::" + key + ">");
  }

  @SuppressWarnings("resource")
  public static Image getImage(Class<?> cls, String key) {
    return new Image(cls.getResourceAsStream(cls.getSimpleName() + "_" + key + ".png"));
  }

  public static boolean getBoolean(Class<?> cls, String key, boolean defaultValue) {
    return getResourceBundle(cls)
      .map(rb -> rb.getObject(key))
      .map(o -> o instanceof Boolean b ? b : Boolean.parseBoolean((String)o))
      .ignore(MissingResourceException.class)
      .orElse(defaultValue);
  }

  public static boolean getBoolean(Class<?> cls, String key) {
    return getBoolean(cls, key, false);
  }

  public static double getDouble(Class<?> cls, String key, double defaultValue) {
    return getResourceBundle(cls)
      .map(rb -> rb.getObject(key))
      .map(o -> o instanceof Number n ? n.doubleValue() : Double.parseDouble((String)o))
      .ignore(MissingResourceException.class)
      .orElse(defaultValue);
  }

  public static double getDouble(Class<?> cls, String key) {
    return getDouble(cls, key, Double.NaN);
  }

  private static Exceptional<ResourceBundle> getResourceBundle(Class<?> cls) {
    return Exceptional.from(() -> DATA.computeIfAbsent(cls, k -> ResourceBundle.getBundle(cls.getName(), Locale.getDefault(), cls.getClassLoader(), YAML_RESOURCE_BUNDLE_CONTROL)));
  }

  private static class YamlControl extends ResourceBundle.Control {
    @Override
    public List<String> getFormats(String baseName) {
      if(baseName == null) {
        throw new NullPointerException();
      }

      return List.of("yaml", "java.class", "java.properties");
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
      if(baseName == null || locale == null || format == null || loader == null) {
        throw new NullPointerException();
      }

      if(format.equals("yaml")) {
        String bundleName = toBundleName(baseName, locale);
        String resourceName = toResourceName(bundleName, format);

//        System.out.println("bundleName = " + bundleName + "; resourceName = " + resourceName);

        try(InputStream resourceAsStream = loader.getResourceAsStream(resourceName)) {
          if(resourceAsStream != null) {
            try {

              @SuppressWarnings("unchecked")
              Map<String, Object> data = OBJECT_MAPPER.readValue(resourceAsStream, Map.class);
              Map<String, Object> flatMap = flatten(data);
//    System.out.println("Mapped succesfully: " + flatMap);
              return new ResourceBundle() {
                @Override
                protected Object handleGetObject(String key) {
                  return flatMap.get(key);
                }

                @Override
                public Enumeration<String> getKeys() {
                  Iterator<String> iterator = flatMap.keySet().iterator();

                  return new Enumeration<>() {
                    @Override
                    public boolean hasMoreElements() {
                      return iterator.hasNext();
                    }

                    @Override
                    public String nextElement() {
                      return iterator.next();
                    }
                  };
                }
              };
            }
            catch(Exception e) {
              e.printStackTrace();
              throw e;
            }
          }
        }
      }

      return super.newBundle(baseName, locale, format, loader, reload);
    }

    private static Map<String, Object> flatten(Map<String, Object> input) {
      Map<String, Object> flatMap = new HashMap<>();

      flatten(flatMap, null, input);

      return flatMap;
    }

    @SuppressWarnings("unchecked")
    private static void flatten(Map<String, Object> flatMap, String prefix, Map<String, Object> input) {
      for(Entry<String, Object> entry : input.entrySet()) {
        String key = prefix == null ? entry.getKey() : prefix + "." + entry.getKey();

        if(entry.getValue() instanceof Map) {
          flatten(flatMap, key, (Map<String, Object>)entry.getValue());
        }
        else {
          flatMap.put(key, entry.getValue());
        }
      }
    }
  }
}
