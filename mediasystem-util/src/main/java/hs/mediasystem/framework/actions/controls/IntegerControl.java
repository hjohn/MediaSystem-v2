package hs.mediasystem.framework.actions.controls;

import hs.mediasystem.framework.actions.Formatter;

public class IntegerControl extends AbstractControl<Long> {
  private final ValueRestrictions<Long> restrictions;
  private final Formatter<Long> formatter;

  public IntegerControl(long initialValue, ValueRestrictions<Long> restrictions, Formatter<Long> formatter) {
    super(initialValue);

    if(restrictions == null) {
      throw new IllegalArgumentException("restrictions cannot be null");
    }

    this.restrictions = restrictions;
    this.formatter = formatter;
  }

  public ValueRestrictions<Long> getRestrictions() {
    return restrictions;
  }

  @Override
  public Formatter<Long> getFormatter() {
    return formatter;
  }
}
