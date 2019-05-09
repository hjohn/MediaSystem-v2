package hs.mediasystem.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;

public class PostConstructCaller {

  public static void call(Object obj) {
    for(Method method : obj.getClass().getDeclaredMethods()) {
      if(method.getAnnotation(PostConstruct.class) != null) {
        try {
          method.setAccessible(true);
          method.invoke(obj);
        }
        catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          throw new IllegalStateException(e);
        }
      }
    }
  }
}
