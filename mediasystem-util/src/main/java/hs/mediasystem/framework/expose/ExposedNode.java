package hs.mediasystem.framework.expose;

import java.util.function.Function;

import javafx.beans.property.Property;

public class ExposedNode<P, T> extends AbstractExposedProperty<P, T> {
  private Class<T> providedType;

  ExposedNode(Function<P, Property<T>> function) {
    super(function);
  }

  public Class<T> getProvidedType() {
    return providedType;
  }

  public class ParentBuilder {
    public ProvidesBuilder of(Class<P> cls) {
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
