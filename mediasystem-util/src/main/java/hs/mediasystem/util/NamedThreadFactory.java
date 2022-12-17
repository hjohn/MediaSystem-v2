package hs.mediasystem.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
  private final AtomicInteger threadNumber = new AtomicInteger(0);
  private final ThreadGroup group;

  private final int priority;
  private final boolean daemon;

  public NamedThreadFactory(String groupName, int priority, boolean daemon) {
    ThreadGroup parentGroup = Thread.currentThread().getThreadGroup();

    this.group = new ThreadGroup(parentGroup, groupName);
    this.daemon = daemon;
    this.priority = priority;
  }

  public NamedThreadFactory(String name) {
    this(name, Thread.NORM_PRIORITY);
  }

  public NamedThreadFactory(String name, boolean daemon) {
    this(name, Thread.NORM_PRIORITY, daemon);
  }

  public NamedThreadFactory(String name, int priority) {
    this(name, priority, false);
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread thread = new Thread(group, r, String.format("%s-%d", group.getName(), threadNumber.incrementAndGet()));

    if(daemon != thread.isDaemon()) {
      thread.setDaemon(daemon);
    }

    if(priority != thread.getPriority()) {
      thread.setPriority(priority);
    }

    return thread;
  }

  public ThreadGroup getThreadGroup() {
    return group;
  }
}