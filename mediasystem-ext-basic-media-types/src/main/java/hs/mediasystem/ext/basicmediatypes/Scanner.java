package hs.mediasystem.ext.basicmediatypes;

import java.nio.file.Path;
import java.util.List;

public interface Scanner<S extends MediaStream<?>> {
  List<S> scan(List<Path> roots);
}
