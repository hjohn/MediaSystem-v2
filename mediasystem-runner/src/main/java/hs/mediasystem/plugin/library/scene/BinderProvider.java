package hs.mediasystem.plugin.library.scene;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BinderProvider {
  @Inject private List<BinderBase<?>> binders;

  public <B> Optional<B> findBinder(Class<B> bindersClass, Class<?> type) {
    return binders.stream()
      .filter(b -> bindersClass.isAssignableFrom(b.getClass()))
      .filter(b -> b.getType().equals(type))
      .map(bindersClass::cast)
      .findFirst();
  }

  public <B, T, R> R map(Class<B> bindersClass, BiFunction<B, T, R> method, T value) {
    B binder = findBinder(bindersClass, value.getClass()).orElseThrow(() -> new IllegalArgumentException(bindersClass + " missing for type: " + value.getClass()));

    return method.apply(binder, value);
  }
}
