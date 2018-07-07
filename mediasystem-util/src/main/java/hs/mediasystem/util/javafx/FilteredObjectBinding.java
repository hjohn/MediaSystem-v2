package hs.mediasystem.util.javafx;

import java.util.function.Predicate;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;

public class FilteredObjectBinding<T> extends ObjectBinding<T> {
  private final Predicate<T> matcher;
  private final ObjectProperty<T> source;

  private T lastValidValue;

  public FilteredObjectBinding(ObjectProperty<T> source, Predicate<T> matcher) {
    this.source = source;
    this.matcher = matcher;

    bind(source);
  }

  @Override
  protected T computeValue() {
    T value = source.get();

    if(matcher.test(value)) {
      lastValidValue = value;

      return value;
    }

    return lastValidValue;
  }
}
