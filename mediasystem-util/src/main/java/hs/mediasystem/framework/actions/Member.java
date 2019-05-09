package hs.mediasystem.framework.actions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.eclipse.jdt.annotation.Nullable;

public class Member {
  private final Field field;
  private final Method method;
  private final boolean internal;

  public Member(Field field, boolean internal) {
    this.field = field;
    this.method = null;
    this.internal = internal;
  }

  public Member(Method method, boolean internal) {
    this.field = null;
    this.method = method;
    this.internal = internal;
  }

  public boolean isInternal() {
    return internal;
  }

  public Method getMethod() {
    return method;
  }

  public Class<?> getType() {
    return field != null ? field.getType() : method.getReturnType();
  }

  public Type getGenericType() {
    return field != null ? field.getGenericType() : method.getGenericReturnType();
  }

  public Class<?> getDeclaringClass() {
    return field != null ? field.getDeclaringClass() : method.getDeclaringClass();
  }

  @Nullable
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return field != null ? field.getAnnotation(annotationClass) : method.getAnnotation(annotationClass);
  }

  public Annotation[] getAnnotations() {
    return field != null ? field.getAnnotations() : method.getAnnotations();
  }

  public String getName() {
    return field != null ? field.getName() : method.getName();
  }

  public Object get(Object root) {
    try {
      return field != null ? field.get(root) : method.invoke(root);
    }
    catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException("Exception accessing: " + toString(), e);
    }
  }

  @Override
  public String toString() {
    return "Member[" + (field != null ? field : method) + "]";
  }
}
