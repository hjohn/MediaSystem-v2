package hs.mediasystem.runner.grouping;

import java.util.List;

public class NoGrouping<T> implements Grouping<T> {

  @SuppressWarnings("unchecked")
  @Override
  public List<Object> group(List<T> items) {
    return (List<Object>)items;
  }
}
