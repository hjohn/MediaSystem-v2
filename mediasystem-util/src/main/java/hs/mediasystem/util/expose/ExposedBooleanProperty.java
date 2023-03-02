package hs.mediasystem.util.expose;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.Property;

public class ExposedBooleanProperty extends AbstractExposedProperty<Boolean> {
  private boolean isTriState;
  private boolean allowSelectTriState;

  ExposedBooleanProperty(Function<Object, Property<Boolean>> function) {
    super(function);
  }

  public void toggle(Object ownerInstance) {
    Property<Boolean> property = getProperty(ownerInstance);

    property.setValue(!property.getValue());
  }

  public boolean isTriState() {
    return isTriState;
  }

  public boolean allowSelectTriState() {
    return allowSelectTriState;
  }

  public class ParentBuilder<O> {
    public RangeBuilder of(Class<O> cls) {
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
