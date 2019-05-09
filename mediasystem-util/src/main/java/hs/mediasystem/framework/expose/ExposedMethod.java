package hs.mediasystem.framework.expose;

import java.util.ArrayList;
import java.util.function.BiFunction;

import javafx.concurrent.Task;
import javafx.event.Event;

public class ExposedMethod<P, V> extends AbstractExposedControl<P> {
  private final BiFunction<P, Event, Task<V>> function;

  public ExposedMethod(BiFunction<P, Event, Task<V>> function) {
    this.function = function;
  }

  public Task<V> call(P parent, Event event) {
    return function.apply(parent, event);
  }

  @Override
  public Class<?> getProvidedType() {
    return null;
  }

  public class ActionParentBuilder {
    public NameBuilder of(Class<? super P> cls) {
      ExposedMethod.this.cls = cls;
      ExposedMethod.this.type = Type.METHOD;

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

