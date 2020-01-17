package hs.mediasystem.ext.vlc.util;

import hs.mediasystem.util.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class BeanAccessor<T> implements Accessor<T> {
  private final Object instance;
  private final String[] propertyNames;
  private final Method[] readMethods;
  private final String writeMethodName;

  private Method writeMethod;

  public BeanAccessor(String writeMethodName, Object instance, String... propertyNames) {
    this.writeMethodName = writeMethodName == null ? propertyNames[propertyNames.length - 1] : writeMethodName;
    this.instance = instance;
    this.propertyNames = propertyNames;

    readMethods = new Method[propertyNames.length];
  }

  public BeanAccessor(Object instance, String... propertyNames) {
    this(null, instance, propertyNames);
  }

  private Object findCurrentInstance() throws IllegalAccessException, InvocationTargetException {
    Object currentInstance = instance;
    int index = 0;

    while(currentInstance != null) {
      if(readMethods[index] == null) {
        readMethods[index] = BeanUtils.getReadMethod(currentInstance.getClass(), propertyNames[index]);
      }

      if(index == propertyNames.length - 1) {
        break;
      }

      currentInstance = readMethods[index++].invoke(currentInstance);
    }

    return currentInstance;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T read() {
    try {
      Object currentInstance = findCurrentInstance();

      if(currentInstance != null) {
        return (T)readMethods[propertyNames.length - 1].invoke(currentInstance);
      }
    }
    catch(InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    return null;
  }

  @Override
  public void write(T value) {
    try {
      Object currentInstance = findCurrentInstance();

      if(currentInstance == null) {
        throw new RuntimeException("property " + Arrays.toString(propertyNames) + " of " + instance + " cannot be written.");
      }

      if(writeMethod == null) {
        writeMethod = BeanUtils.getWriteMethod(currentInstance.getClass(), writeMethodName, readMethods[propertyNames.length - 1].getReturnType());
      }

      writeMethod.invoke(currentInstance, value);
    }
    catch(InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
