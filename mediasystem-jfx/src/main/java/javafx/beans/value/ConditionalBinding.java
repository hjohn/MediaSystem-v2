package javafx.beans.value;

import com.sun.javafx.binding.Subscription;

import java.util.Objects;

public class ConditionalBinding<T> extends LazyObjectBinding<T> {
  private final ObservableValue<Boolean> condition;
  private final ObservableValue<T> source;

  private Subscription subscription;

  public ConditionalBinding(ObservableValue<T> source, ObservableValue<Boolean> condition) {
    this.source = Objects.requireNonNull(source);
    this.condition = Objects.requireNonNull(condition);

    condition.subscribeInvalidations(() -> {
      invalidate();

      if(!isActive()) {
        getValue();  // make valid so last value is cached while conditional is false
      }
    });
  }

  @Override
  protected boolean allowValidation() {
    // This binding is valid when it is itself observed, or is currently inactive.
    // When inactive, the binding has the value of its source at the time it became
    // inactive.
    return super.allowValidation() || !isActive();
  }

  @Override
  protected T computeValue() {
    unsubscribe();

    if(isObserved() && isActive()) {
      subscription = source.subscribeInvalidations(this::invalidate);
    }

    return source.getValue();
  }

  @Override
  protected Subscription observeInputs() {
    return this::unsubscribe;  // condition is always observed and never unsubscribed
  }

  private boolean isActive() {
    return Boolean.TRUE.equals(condition.getValue());
  }

  private void unsubscribe() {
    if(subscription != null) {
      subscription.unsubscribe();
      subscription = null;
    }
  }
}
