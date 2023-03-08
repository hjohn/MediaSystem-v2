package hs.mediasystem.runner.util.debug;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DebugFX {
  private static final Map<Object, Object> MAP = new WeakHashMap<>();
  private static final AtomicBoolean ENABLED = new AtomicBoolean();

  public static void addReference(Object reference) {
    MAP.put(reference, null);
  }

  public static void checkReferences() {
    System.gc();

    Map<Class<?>, List<Object>> collect = MAP.keySet().stream().collect(Collectors.groupingBy(Object::getClass));

    for(Map.Entry<Class<?>, List<Object>> entry : collect.entrySet()) {
      if(entry.getValue().size() > 1) {
        System.out.println("Leak: " + entry.getKey() + ": Found " + entry.getValue().size() + ": " + entry.getValue());
      }
    }
  }

  public static boolean getEnabled() {
    return ENABLED.get();
  }

  public static void setEnabled(boolean enabled) {
    ENABLED.set(enabled);
  }

  static {
    Thread t = new Thread(() -> {
      try {
        for(;;) {
          Thread.sleep(15000);

          if(ENABLED.get()) {
            checkReferences();
          }
        }
      }
      catch(InterruptedException e) {
        e.printStackTrace();
      }
    });

    t.setName("DebugFX");
    t.setDaemon(true);
    t.start();
  }
}
