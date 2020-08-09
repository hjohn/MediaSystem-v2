package hs.database.core;

import java.util.List;

public interface Fetcher<P, C> {
  List<C> fetch(P parent);
}
