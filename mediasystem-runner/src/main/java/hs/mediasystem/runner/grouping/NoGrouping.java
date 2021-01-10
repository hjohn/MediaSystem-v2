package hs.mediasystem.runner.grouping;

import java.util.List;

public class NoGrouping<T, U> implements Grouping<T, U> {

  @SuppressWarnings("unchecked")
  @Override
  public List<U> group(List<? extends T> items) {
    return (List<U>)items;
  }
}
