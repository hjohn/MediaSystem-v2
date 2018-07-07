package hs.mediasystem.util.javafx;

import hs.mediasystem.util.BeanUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

public class MapBindings {
  public static StringBinding selectString(final Observable root, final Object... steps) {
    return new StringBinding() {
      private final Helper helper;

      {
        helper = new Helper(this, root, steps);
      }

      @Override
      public void dispose() {
        helper.unregisterListeners();
      }

      @Override
      protected void onInvalidating() {
        helper.unregisterListeners();
      }

      @Override
      protected String computeValue() {
        Object obj = helper.computeValue();

        return obj instanceof String ? (String)obj : null;
      }
    };
  }

  public static IntegerBinding selectInteger(final Observable root, final Object... steps) {
    return new IntegerBinding() {
      private final Helper helper;

      {
        helper = new Helper(this, root, steps);
      }

      @Override
      public void dispose() {
        helper.unregisterListeners();
      }

      @Override
      protected void onInvalidating() {
        helper.unregisterListeners();
      }

      @Override
      protected int computeValue() {
        Object obj = helper.computeValue();

        return obj instanceof Number ? ((Number)obj).intValue() : 0;
      }
    };
  }

  public static LongBinding selectLong(final Observable root, final Object... steps) {
    return new LongBinding() {
      private final Helper helper;

      {
        helper = new Helper(this, root, steps);
      }

      @Override
      public void dispose() {
        helper.unregisterListeners();
      }

      @Override
      protected void onInvalidating() {
        helper.unregisterListeners();
      }

      @Override
      protected long computeValue() {
        Object obj = helper.computeValue();

        return obj instanceof Number ? ((Number)obj).longValue() : 0;
      }
    };
  }

  public static DoubleBinding selectDouble(final Observable root, final Object... steps) {
    return new DoubleBinding() {
      private final Helper helper;

      {
        helper = new Helper(this, root, steps);
      }

      @Override
      public void dispose() {
        helper.unregisterListeners();
      }

      @Override
      protected void onInvalidating() {
        helper.unregisterListeners();
      }

      @Override
      protected double computeValue() {
        Object obj = helper.computeValue();

        return obj instanceof Number ? ((Number)obj).doubleValue() : 0;
      }
    };
  }

  public static BooleanBinding selectBoolean(final Observable root, final Object... steps) {
    return new BooleanBinding() {
      private final Helper helper;

      {
        helper = new Helper(this, root, steps);
      }

      @Override
      public void dispose() {
        helper.unregisterListeners();
      }

      @Override
      protected void onInvalidating() {
        helper.unregisterListeners();
      }

      @Override
      protected boolean computeValue() {
        Object obj = helper.computeValue();

        return obj instanceof Boolean ? ((Boolean)obj).booleanValue() : false;
      }
    };
  }

  public static <T> ObjectBinding<T> select(final Observable root, final Object... steps) {
    return new ObjectBinding<T>() {
      private final Helper helper;

      {
        helper = new Helper(this, root, steps);
      }

      @Override
      public void dispose() {
        helper.unregisterListeners();
      }

      @Override
      protected void onInvalidating() {
        helper.unregisterListeners();
      }

      @SuppressWarnings("unchecked")
      @Override
      protected T computeValue() {
        return (T)helper.computeValue();
      }
    };
  }

  public static Builder get(Observable root) {
    return new Builder(root);
  }

  private static class Helper {
    private final Observable[] observables;
    private final Property[] properties;
    private final InvalidationListener listener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        binding.invalidate();
      }
    };
    private final WeakInvalidationListener observer = new WeakInvalidationListener(listener);
    private final Observable root;
    private final Step[] steps;
    private final Binding<?> binding;

    public Helper(Binding<?> binding, final Observable root, final Object... steps) {
      this.binding = binding;
      this.root = root;
      this.steps = expandSteps(root, steps);
      observables = new Observable[this.steps.length + 1];
      properties = new Property[this.steps.length];
    }

    private static Step[] expandSteps(Observable root, Object[] steps) {
      List<Object> expandedSteps = new ArrayList<>();
      int indicesRequired = root instanceof ObservableValue ? 0 : 1;

      for(int i = 0; i < steps.length; i++) {
        Object step = steps[i];

        if(indicesRequired-- <= 0) {
          if(step instanceof Step) {
            expandedSteps.add(step);
            continue;
          }

          if(!(step instanceof String)) {
            throw new IllegalArgumentException("expected String at step " + i + ": " + step);
          }

          indicesRequired = 0;

          for(String subStep : ((String)step).split("\\.")) {
            if(!subStep.matches("[_a-z][_A-Za-z0-9]*(\\[\\])?")) {
              throw new IllegalArgumentException("invalid step format at step " + i + ": " + step);
            }

            if(subStep.endsWith("[]")) {
              expandedSteps.add(new Then(subStep.substring(0, subStep.length() - 2)));
              indicesRequired++;
            }
            else {
              expandedSteps.add(new Then(subStep));
            }
          }
        }
        else {
          expandedSteps.add(new IndexLookup(step, i));
        }
      }

      if(indicesRequired > 0) {
        throw new IllegalArgumentException("insufficient indices specified in steps: missing " + indicesRequired + " indices");
      }

      return expandedSteps.toArray(new Step[expandedSteps.size()]);
    }

    private void unregisterListeners() {
      for(int index = 0; index < observables.length; index++) {
        if(observables[index] == null) {
          break;
        }
        observables[index].removeListener(this.observer);
        observables[index] = null;
      }
    }

    protected Object computeValue() {
      Observable observable = root;

      for(int index = 0; index <= steps.length; index++) {
        observables[index] = observable;
        observable.addListener(observer);

        if(index == steps.length) {
          break;
        }

        Object value = observable instanceof ObservableValue ? ((ObservableValue<?>)observable).getValue() : observable;

        try {
          if(properties[index] == null || !value.getClass().equals(properties[index].getBeanClass())) {
            properties[index] = new Property(value.getClass(), steps[index]);
          }

          observable = properties[index].getObservable(value);
        }
        catch(RuntimeBindException e) {
          throw e;
        }
        catch(RuntimeException e) {
          return null;
        }
      }

      return ((ObservableValue<?>)observable).getValue();
    }
  }

  private static class Property {
    private final Class<?> cls;
    private final Step step;

    public Property(Class<?> cls, Step name) {
      this.cls = cls;
      this.step = name;
    }

    public Observable getObservable(Object bean) {
      try {
        return step.execute(cls, bean);
      }
      catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        e.printStackTrace();
        return null;
      }
    }

    public Class<?> getBeanClass() {
      return cls;
    }
  }

  private interface Step {
    Observable execute(Class<?> cls, Object bean) throws IllegalAccessException, InvocationTargetException;
  }

  private static class Then implements Step {
    private final String propertyName;

    private Method method;
    private Field field;

    public Then(String propertyName) {
      this.propertyName = propertyName;
    }

    @Override
    public Observable execute(Class<?> cls, Object bean) throws IllegalAccessException, InvocationTargetException {
      if(method == null && field == null) {
        try {
          method = cls.getMethod(propertyName + "Property");
        }
        catch(NoSuchMethodException e) {
          try {
            field = cls.getField(propertyName);
          }
          catch(NoSuchFieldException e2) {
            try {
              method = cls.getMethod("get" + BeanUtils.capitalize(propertyName));

              if(method.getReturnType().equals(String.class)) {
                return new SimpleStringProperty((String)method.invoke(bean));
              }

              return new SimpleObjectProperty<>(method.invoke(bean));
            }
            catch(NoSuchMethodException e3) {
              throw new RuntimeBindException("No such property found: '" + propertyName + "' in class: " + cls);
            }
          }
        }
      }

      return method != null ? (Observable)method.invoke(bean) : (Observable)field.get(bean);
    }

    @Override
    public String toString() {
      return "Then[" + propertyName + "]";
    }
  }

  private static class IndexLookup implements Step {
    private final Object obj;
    private final int stepNumber;

    public IndexLookup(Object obj, int stepNumber) {
      this.obj = obj;
      this.stepNumber = stepNumber;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable execute(Class<?> cls, Object bean) throws IllegalAccessException, InvocationTargetException {
      if(bean instanceof ObservableMap) {
        return Bindings.valueAt((ObservableMap<Object, ?>)bean, obj);
      }

      if(bean instanceof ObservableList) {
        if(!(obj instanceof Integer)) {
          throw new RuntimeBindException("index for ObservableList (" + bean + ") must be of type Integer: " + obj.getClass());
        }

        int index = (Integer)obj;

        if(index < 0) {
          throw new RuntimeBindException("index for ObservableList (" + bean + ") cannot be negative: " + index);
        }

        ObservableList<?> observableList = (ObservableList<?>)bean;

        // Special binding to avoid an IndexOutOfBoundsException being logspammed by JavaFX when the ObservableList is empty [JDK-8090660]
        return Bindings.createObjectBinding(() -> observableList.isEmpty() ? null : observableList.get(index), observableList);
      }

      throw new RuntimeBindException("map or list expected at step " + (stepNumber - 1) + ", but got: " + cls + ": " + bean);
    }

    @Override
    public String toString() {
      return "IndexLookup[" + obj + "]";
    }
  }

  public static class Builder {
    private final Observable root;
    private final List<Object> steps = new ArrayList<>();

    public Builder(Observable root) {
      this.root = root;
    }

    public Builder then(String propertyName) {
      steps.add(propertyName);
      return this;
    }

    public Builder thenOrDefault(String propertyName, Object defaultValue) {
      steps.add(new ThenOrDefault(propertyName, defaultValue));
      return this;
    }

    public Builder lookup(Object valueOrIndex) {
      steps.add(new IndexLookup(valueOrIndex, steps.size()));
      return this;
    }

    public StringBinding asStringBinding() {
      return MapBindings.selectString(root, steps.toArray(new Object[steps.size()]));
    }

    public <T> ObjectBinding<T> asObjectBinding() {
      return MapBindings.select(root, steps.toArray(new Object[steps.size()]));
    }

    private static class ThenOrDefault implements Step {
      private final String propertyName;
      private final Object defaultValue;

      private Method method;
      private Field field;

      public ThenOrDefault(String propertyName, Object defaultValue) {
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
      }

      @Override
      public Observable execute(Class<?> cls, Object bean) throws IllegalAccessException, InvocationTargetException {
        if(method == null && field == null) {
          try {
            method = cls.getMethod(propertyName + "Property");
          }
          catch(NoSuchMethodException e) {
            try {
              field = cls.getField(propertyName);
            }
            catch(NoSuchFieldException e2) {
              return new ObservableValue<Object>() {
                @Override
                public void addListener(InvalidationListener listener) {
                }

                @Override
                public void removeListener(InvalidationListener listener) {
                }

                @Override
                public void addListener(ChangeListener<? super Object> listener) {
                }

                @Override
                public void removeListener(ChangeListener<? super Object> listener) {
                }

                @Override
                public Object getValue() {
                  return defaultValue;
                }
              };
            }
          }
        }

        return method != null ? (Observable)method.invoke(bean) : (Observable)field.get(bean);
      }
    }
  }
}
