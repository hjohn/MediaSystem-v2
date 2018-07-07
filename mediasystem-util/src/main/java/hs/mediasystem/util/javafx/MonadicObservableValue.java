package hs.mediasystem.util.javafx;

import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.value.ObservableValue;

public interface MonadicObservableValue<T> extends ObservableValue<T> {

  default <U> MonadicObjectBinding<U> map(Function<? super T, ? extends U> f) {
    return new MonadicObjectBinding<U>() {
      {
        bind(MonadicObservableValue.this);
      }

      @Override
      protected U computeValue() {
        T value = MonadicObservableValue.this.getValue();

        return value == null ? null : f.apply(value);
      }
    };
  }

  default MonadicObjectBinding<T> orElse(T other) {
    return new MonadicObjectBinding<T>() {
      {
        bind(MonadicObservableValue.this);
      }

      @Override
      protected T computeValue() {
        T value = MonadicObservableValue.this.getValue();

        return value == null ? other : value;
      }
    };
  }

  default MonadicObjectBinding<T> filter(Predicate<T> predicate) {
    return new MonadicObjectBinding<T>() {
      {
        bind(MonadicObservableValue.this);
      }

      @Override
      protected T computeValue() {
        T value = MonadicObservableValue.this.getValue();

        return predicate.test(value) ? value : null;
      }
    };
  }
}
