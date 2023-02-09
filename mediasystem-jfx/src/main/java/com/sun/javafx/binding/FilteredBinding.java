package com.sun.javafx.binding;

import java.util.Objects;
import java.util.function.Predicate;

import javafx.beans.value.ObservableValue;

public class FilteredBinding<T> extends LazyObjectBinding<T> {

    private final ObservableValue<T> source;
    private final Predicate<? super T> predicate;

    public FilteredBinding(ObservableValue<T> source, Predicate<? super T> predicate) {
        this.source = Objects.requireNonNull(source);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    protected Subscription observeSources() {
        return Subscription.subscribeInvalidations(source, this::invalidate); // start observing source
    }

    @Override
    protected T computeValue() {
        T value = source.getValue();

        return value == null ? null
            : predicate.test(value) ? value
            : null;
    }
}