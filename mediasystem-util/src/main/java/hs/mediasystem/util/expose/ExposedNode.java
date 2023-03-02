package hs.mediasystem.util.expose;

import java.util.function.Function;

import javafx.beans.property.Property;

public class ExposedNode<T> extends AbstractExposedProperty<T> {
  private Class<T> providedType;

  ExposedNode(Function<Object, Property<T>> function) {
    super(function);
  }

  public Class<T> getProvidedType() {
    return providedType;
  }

  public class ParentBuilder<O> {
    public ProvidesBuilder of(Class<O> cls) {
      ExposedNode.this.cls = cls;

      return new ProvidesBuilder();
    }
  }

  public class ProvidesBuilder {
    public NameBuilder provides(Class<T> cls) {
      ExposedNode.this.providedType = cls;

      return new NameBuilder();
    }
  }
}
