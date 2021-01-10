package hs.mediasystem.runner.grouping;

import java.util.List;

public interface Grouping<T, U> {
  List<U> group(List<? extends T> items);
}
