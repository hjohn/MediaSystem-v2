package hs.mediasystem.util.expose;

import java.util.ArrayList;
import java.util.function.Function;

public class ExposedMethod<V> extends AbstractExposedControl {
  private final Function<Object, Trigger<V>> function;

  public ExposedMethod(Function<Object, Trigger<V>> function) {
    this.function = function;
  }

  /**
   * Returns a Trigger or <code>null</code> if the action is unavailable
   * right now.
   *
   * @param ownerInstance the instance which the method returning the trigger is part of
   * @return a {@link Trigger} or <code>null</code> if the action is unavailable
   */
  public Trigger<V> getTrigger(Object ownerInstance) {
    return function.apply(ownerInstance);
  }

  public class ActionParentBuilder<O> {
    public NameBuilder of(Class<? super O> cls) {
      ExposedMethod.this.cls = cls;

      return new NameBuilder();
    }
  }

  public class NameBuilder {
    public void as(String name) {
      ExposedMethod.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedMethod.this);
    }
  }
}

