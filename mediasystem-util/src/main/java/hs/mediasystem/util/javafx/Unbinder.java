package hs.mediasystem.util.javafx;

import java.util.ArrayList;
import java.util.List;

public class Unbinder {
  private final List<Runnable> removers = new ArrayList<>();

  public void add(Runnable runnable) {
    removers.add(runnable);
  }

  public void unbindAll() {
    System.out.println("^^^^^^^^^ There are " + removers.size() + " removers...");
    try {
      removers.forEach(Runnable::run);
      removers.clear();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    System.out.println("^^^^^^^^^ There are NOW " + removers.size() + " removers...");
  }
}
