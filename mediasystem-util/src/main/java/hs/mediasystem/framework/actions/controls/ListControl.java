package hs.mediasystem.framework.actions.controls;

import hs.mediasystem.framework.actions.Formatter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ListControl<T> extends AbstractControl<T> {
  private final ObservableList<T> allowedValues;
  private final Formatter<T> formatter;

  public ListControl(T initialValue, ObservableList<T> allowedValues, Formatter<T> formatter) {
    super(initialValue);

    this.allowedValues = FXCollections.unmodifiableObservableList(allowedValues);
    this.formatter = formatter;
  }

  @Override
  public void setValue(T value) {
    if(!allowedValues.contains(value)) {
      throw new IllegalArgumentException("Value out of range: " + value);
    }

    super.setValue(value);
  }

  /**
   * Returns an unmodifiable observable list of allowed values.
   *
   * @return an unmodifiable observable list of allowed values, never null
   */
  public ObservableList<T> getAllowedValues() {
    return allowedValues;
  }

  @Override
  public Formatter<T> getFormatter() {
    return formatter;
  }
}
