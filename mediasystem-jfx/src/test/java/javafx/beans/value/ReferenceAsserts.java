package javafx.beans.value;

import java.lang.ref.WeakReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ReferenceAsserts {

  @SuppressWarnings("all")
  public static void testIfStronglyReferenced(Object obj, Runnable clearRefs) {
    WeakReference<Object> ref = new WeakReference<>(obj);

    clearRefs.run();
    obj = null;

    System.gc();

    assertNotNull(ref.get());
  }

  @SuppressWarnings("all")
  public static void testIfNotStronglyReferenced(Object obj, Runnable clearRefs) {
    WeakReference<Object> ref = new WeakReference<>(obj);

    clearRefs.run();
    obj = null;

    System.gc();

    assertNull(ref.get());
  }
}
