package hs.mediasystem.runner.grouping;

import java.util.List;

public interface Grouping<T> {
  List<Object> group(List<T> items);
}
