package hs.database.util;

import java.util.ArrayList;
import java.util.List;

public class Closer {
  private final List<AutoCloseable> closeList = new ArrayList<>();

  public <T extends AutoCloseable> T add(T autoCloseable) {
    closeList.add(closeList.size(), autoCloseable);

    return autoCloseable;
  }

  public void closeAll() throws Exception {
    Exception exception = null;

    while(!closeList.isEmpty()) {
      try(AutoCloseable autoCloseable = closeList.remove(closeList.size() - 1)) {
      }
      catch(Exception e) {
        if(exception != null) {
          exception.addSuppressed(e);
        }
        else {
          exception = e;
        }
      }
    }

    if(exception != null) {
      throw exception;
    }
  }
}
