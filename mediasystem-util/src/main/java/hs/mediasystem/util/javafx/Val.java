package hs.mediasystem.util.javafx;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;

public interface Val<T> extends ObservableValue<T> {

  default <U> MonadicObjectBinding<U> map(Function<? super T, ? extends U> f) {
    return new MonadicObjectBinding<>() {
      {
        bind(Val.this);
      }

      @Override
      protected U computeValue() {
        T value = Val.this.getValue();

        return value == null ? null : f.apply(value);
      }
    };
  }

  default <U> MonadicObjectBinding<U> flatMap(Function<? super T, ? extends ObservableValue<U>> mapper) {
    Objects.requireNonNull(mapper);

    return new MonadicObjectBinding<>() {
      private final InvalidationListener listener = obs -> invalidate();

      private Runnable remover;

      {
        bind(Val.this);
      }

      @Override
      protected U computeValue() {
        T value = Val.this.getValue();

        if(remover != null) {
          remover.run();
          remover = null;
        }

        if(value == null) {
          return null;
        }

        ObservableValue<U> apply = mapper.apply(value);

        apply.addListener(listener);
        remover = () -> apply.removeListener(listener);

        return apply.getValue();
      }
    };
  }
  /*
  public <U> Optional<U> flatMap(Function<? super T, ? extends Optional<? extends U>> mapper) {
    Objects.requireNonNull(mapper);
    if (!isPresent()) {
        return empty();
    } else {
        @SuppressWarnings("unchecked")
        Optional<U> r = (Optional<U>) mapper.apply(value);
        return Objects.requireNonNull(r);
    }
}
*/
  default MonadicObjectBinding<T> orElse(ObservableValue<T> other) {
    return new MonadicObjectBinding<>() {
      {
        bind(Val.this);
      }

      @Override
      protected T computeValue() {
        T value = Val.this.getValue();

        return value == null ? other.getValue() : value;
      }
    };
  }

  default MonadicObjectBinding<T> orElse(T other) {
    return new MonadicObjectBinding<>() {
      {
        bind(Val.this);
      }

      @Override
      protected T computeValue() {
        T value = Val.this.getValue();

        return value == null ? other : value;
      }
    };
  }

  /**
   * Returns a new {@link Val} that holds the same value as this when
   * the value statisfies the predicate, and is empty when this is empty
   * or its value does not satisfy the predicate.
   *
   * @param predicate a predicate to satisfy
   * @return a new {@link Val}, never null
   */
  default MonadicObjectBinding<T> filter(Predicate<T> predicate) {
    return new MonadicObjectBinding<>() {
      {
        bind(Val.this);
      }

      @Override
      protected T computeValue() {
        T value = Val.this.getValue();

        return value != null && predicate.test(value) ? value : null;
      }
    };
  }

  /**
   * Returns a new {@link Val} that holds the same value as this when
   * the condition evaluates to <code>true</code>, and is empty when this is empty
   * or the condition evaluated to <code>false</code>.
   *
   * @param condition a condition to evaluate
   * @return a new {@link Val}, never null
   */
  default MonadicObjectBinding<T> filter(ObjectBinding<Boolean> condition) {
    return new MonadicObjectBinding<>() {
      {
        bind(Val.this, condition);
      }

      @Override
      protected T computeValue() {
        T value = Val.this.getValue();

        return condition.get() ? value : null;
      }
    };
  }

  default void ifPresent(Consumer<T> consumer) {
    if(getValue() != null) {
      consumer.accept(getValue());
    }
  }
}
