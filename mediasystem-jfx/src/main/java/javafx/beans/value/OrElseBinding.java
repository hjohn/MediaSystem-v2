package javafx.beans.value;

import com.sun.javafx.binding.Subscription;

import java.util.Objects;

class OrElseBinding<T> extends LazyObjectBinding<T> {

    private final ObservableValue<T> source;
    private final T constant;

    public OrElseBinding(ObservableValue<T> source, T constant) {
        this.source = Objects.requireNonNull(source);
        this.constant = constant;
    }

    @Override
    protected Subscription observeInputs() {
      return Subscription.subscribeInvalidations(source, this::invalidate); // start observing source
    }

    @Override
    protected T computeValue() {
      T value = source.getValue();
      return value == null ? constant : value;
    }
}