package hs.mediasystem.framework.actions;

import java.lang.annotation.RetentionPolicy;

@java.lang.annotation.Documented
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
public @interface Range {
  double min();
  double max();
  double step();
}
