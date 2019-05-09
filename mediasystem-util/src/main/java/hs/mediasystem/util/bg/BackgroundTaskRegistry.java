package hs.mediasystem.util.bg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BackgroundTaskRegistry {
  private static final List<Workload> WORKLOADS = new ArrayList<>();

  public static Workload createWorkload(String description) {
    Workload workload = new Workload(description);

    WORKLOADS.add(workload);

    return workload;
  }

  public static List<Workload> getActiveWorkloads() {
    return WORKLOADS.stream()
      .filter(Workload::isActive)
      .collect(Collectors.toList());
  }

  public static class Workload {
    private final String description;

    private long completed;
    private long total;
    private long endTime;

    Workload(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }

    private void checkReset() {
      if(endTime != 0 && System.currentTimeMillis() - endTime > 10000) {
        reset();
      }
    }

    public synchronized boolean isActive() {
      checkReset();

      return total != completed;
    }

    public synchronized long getCompleted() {
      return completed;
    }

    public synchronized long getTotal() {
      return total;
    }

    public synchronized void start(int count) {
      checkReset();

      endTime = 0;
      total += count;
    }

    public synchronized void start() {
      start(1);
    }

    public synchronized void complete() {
      checkReset();

      if(++completed == total) {
        endTime = System.currentTimeMillis();
      }
    }

    public synchronized void reset() {
      endTime = 0;
      completed = 0;
      total = 0;
    }
  }
}
