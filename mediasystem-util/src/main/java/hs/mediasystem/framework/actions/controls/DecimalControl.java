package hs.mediasystem.framework.actions.controls;

import hs.mediasystem.framework.actions.Formatter;

public class DecimalControl extends AbstractControl<Double> {
  private final ValueRestrictions<Double> restrictions;
  private final Formatter<Double> formatter;

  public DecimalControl(double initialValue, ValueRestrictions<Double> restrictions, Formatter<Double> formatter) {
    super(initialValue);

    if(restrictions == null) {
      throw new IllegalArgumentException("restrictions cannot be null");
    }

    this.restrictions = restrictions;
    this.formatter = formatter;
  }

  public ValueRestrictions<Double> getRestrictions() {
    return restrictions;
  }

  @Override
  public Formatter<Double> getFormatter() {
    return formatter;
  }
}
