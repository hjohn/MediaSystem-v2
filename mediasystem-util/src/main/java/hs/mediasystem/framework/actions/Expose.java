package hs.mediasystem.framework.actions;

import java.lang.annotation.RetentionPolicy;

import javafx.event.Event;

@java.lang.annotation.Documented
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
public @interface Expose {
  String values() default "";
  Class<? extends ValueBuilder<?>> valueBuilder() default NullValueBuilder.class;
//  Class<? extends StringConverter<?>> stringConverter() default ResourceBasedStringConverter.class;
  String name() default "";

//  public static class ResourceBasedStringConverter implements StringConverter<Object> {
//    @Override
//    public String toString(Object object) {
//      return ResourceManager.getText(object.getClass(), "label");
//    }
//  }

  public static class NullValueBuilder implements ValueBuilder<Void> {
    @Override
    public Void build(Event event, Void currentValue) {
      throw new UnsupportedOperationException();
    }
  }
}
