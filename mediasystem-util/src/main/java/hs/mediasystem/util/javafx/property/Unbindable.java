package hs.mediasystem.util.javafx.property;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public interface Unbindable {
  static final WeakHashMap<Object, List<Runnable>> LISTENERS = new WeakHashMap<>();

  void unbind();

  default void unbindAll() {
    unbind();
    releaseBindings();
  }

  default void addBindingRemover(Runnable runnable) {
    LISTENERS.computeIfAbsent(this, k -> new ArrayList<>()).add(runnable);
  }

  default void releaseBindings() {
    LISTENERS.computeIfPresent(this, (k, v) -> {
      v.forEach(Runnable::run);
      return null;
    });
  }
}
