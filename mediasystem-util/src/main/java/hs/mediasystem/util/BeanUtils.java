package hs.mediasystem.util;

import java.lang.reflect.Method;
import java.util.Locale;

public class BeanUtils {

  public static String capitalize(String propertyName) {
    return propertyName.substring(0, 1).toUpperCase(Locale.ENGLISH) + propertyName.substring(1);
  }

  public static Method getReadMethod(Class<?> cls, String propertyName) {
    try {
      Method method = null;

      try {
        method = cls.getMethod("is" + capitalize(propertyName));
      }
      catch(NoSuchMethodException e) {
        method = cls.getMethod("get" + capitalize(propertyName));
      }

      return method;
    }
    catch(NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public static Method getWriteMethod(Class<?> cls, String propertyName, Class<?>... parameterTypes) {
    try {
      return cls.getMethod("set" + capitalize(propertyName), parameterTypes);
    }
    catch(NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public static Method getSetter(Class<?> cls, String propertyName) {
    try {
      String methodName = "set" + capitalize(propertyName);

      for(Method method : cls.getMethods()) {
        if(method.getName().equals(methodName) && method.getParameterTypes().length == 1) {
          return method;
        }
      }

      throw new NoSuchMethodException(methodName);
    }
    catch(NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
}
