package hs.mediasystem.framework.expose;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.Property;

public class ExposedBooleanProperty<P> extends AbstractExposedProperty<P, Boolean> {
  private boolean isTriState;
  private boolean allowSelectTriState;

  ExposedBooleanProperty(Function<P, Property<Boolean>> function) {
    super(function);
  }

  public boolean isTriState() {
    return isTriState;
  }

  public boolean allowSelectTriState() {
    return allowSelectTriState;
  }

  public class ParentBuilder {
    public RangeBuilder of(Class<? super P> cls) {
      ExposedBooleanProperty.this.cls = cls;

      return new RangeBuilder();
    }
  }

  public class RangeBuilder {
    public RangeBuilder asTriState(boolean allowSelectTriState) {
      ExposedBooleanProperty.this.isTriState = true;
      ExposedBooleanProperty.this.allowSelectTriState = allowSelectTriState;

      return this;
    }

    public void as(String name) {
      ExposedBooleanProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedBooleanProperty.this);
    }
  }
}
