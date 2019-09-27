package hs.mediasystem.runner.util;

import java.util.Comparator;
import java.util.function.Predicate;

public class DelegatingComparator<T> implements Comparator<T> {
  private final Predicate<T> usePrimary;
  private final Comparator<T> primaryComparator;
  private final Comparator<T> secondaryComparator;

  public DelegatingComparator(Predicate<T> usePrimary, Comparator<T> primaryComparator, Comparator<T> secondaryComparator) {
    this.usePrimary = usePrimary;
    this.primaryComparator = primaryComparator;
    this.secondaryComparator = secondaryComparator;
  }

  @Override
  public int compare(T o1, T o2) {
    if(usePrimary.test(o1) || usePrimary.test(o2)) {
      return primaryComparator.compare(o1, o2);
    }

    return secondaryComparator.compare(o1, o2);
  }
}