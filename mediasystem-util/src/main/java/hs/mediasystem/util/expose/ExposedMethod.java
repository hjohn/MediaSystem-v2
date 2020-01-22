package hs.mediasystem.util.expose;

import java.util.ArrayList;
import java.util.function.Function;

public class ExposedMethod<P, V> extends AbstractExposedControl<P> {
  private final Function<P, Trigger<V>> function;

  public ExposedMethod(Function<P, Trigger<V>> function) {
    this.function = function;
  }

  /**
   * Returns a Trigger or <code>null</code> if the action is unavailable
   * rigt now.
   *
   * @param parent the parent where the method returning the trigger is part of
   * @return a {@link Trigger} or <code>null</code> if the action is unavailable
   */
  public Trigger<V> getTrigger(P parent) {
    return function.apply(parent);
  }

  public class ActionParentBuilder {
    public NameBuilder of(Class<? super P> cls) {
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

