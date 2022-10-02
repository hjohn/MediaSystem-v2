package hs.database.core;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;

public interface Mapper<T> {
  T map(Object[] data) throws Throwable;

  public static <T> Mapper<T> of(Class<T> cls) {
    @SuppressWarnings("unchecked")
    Constructor<T>[] declaredConstructors = (Constructor<T>[])cls.getDeclaredConstructors();

    if(declaredConstructors.length != 1) {
      throw new IllegalArgumentException("Given class must have exactly one constructor: " + cls);
    }

    return declaredConstructors[0]::newInstance;
  }

  @SuppressWarnings("unchecked")
  public static <T> Mapper<T> of(Lookup lookup, Class<T> cls) {
    Constructor<T>[] declaredConstructors = (Constructor<T>[])cls.getDeclaredConstructors();

    if(declaredConstructors.length != 1) {
      throw new IllegalArgumentException("Given class must have exactly one constructor: " + cls);
    }

    return data -> (T)lookup.unreflectConstructor(declaredConstructors[0]).invokeWithArguments(data);
  }
}
