package hs.mediasystem.runner.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class DebugSceneFX {
  private final WeakHashMap<Node, Integer> referenceCounters = new WeakHashMap<>();
  private final Scene scene;
  private final long checkIntervalMillis;

  public static void monitor(Scene scene, long checkIntervalMillis) {
    DebugSceneFX debugger = new DebugSceneFX(scene, checkIntervalMillis);

    debugger.start();
  }

  private DebugSceneFX(Scene scene, long checkIntervalMillis) {
    this.scene = scene;
    this.checkIntervalMillis = checkIntervalMillis;
  }

  public void start() {
    TrackingListChangeListener listener = new TrackingListChangeListener();

    updateRootListener(listener, null, scene.getRoot());

    // Maintain list of everything attached to Scene
    scene.rootProperty().addListener((obs, old, current) -> updateRootListener(listener, old, current));

    new Thread(this::monitor).start();
  }

  private static void updateRootListener(TrackingListChangeListener listener, Parent old, Parent current) {
    if(old != null) {
      old.getChildrenUnmodifiable().removeListener(listener);
    }
    if(current != null) {
      listener.onChanged(new FakeAddChange(current));

      current.getChildrenUnmodifiable().addListener(listener);
    }
  }

  private void incrementRef(Node node) {
    referenceCounters.merge(node, 1, Integer::sum);
  }

  private void decrementRef(Node node) {
    referenceCounters.merge(node, -1, Integer::sum);
  }

  private class TrackingListChangeListener implements ListChangeListener<Node> {

    @Override
    public void onChanged(Change<? extends Node> c) {
      synchronized(referenceCounters) {
        while(c.next()) {
          for(Node node : c.getRemoved()) {
            decrementRef(node);

            if(node instanceof Parent p) {
              onChanged(new FakeRemoveChange((Parent)node));
              p.getChildrenUnmodifiable().removeListener(this);
            }
          }

          for(Node node : c.getAddedSubList()) {
            incrementRef(node);

            if(node instanceof Parent p) {
              onChanged(new FakeAddChange((Parent)node));
              p.getChildrenUnmodifiable().addListener(this);
            }
          }
        }
      }
    }
  }

  private void monitor() {
    try {
      for(;;) {
        Thread.sleep(checkIntervalMillis);
        System.gc();

        synchronized(referenceCounters) {
          Set<Node> knownParents = new HashSet<>();

          for(Map.Entry<Node, Integer> entry : referenceCounters.entrySet()) {
            if(entry.getValue() == 0) {
              Node node = entry.getKey();

              if(node != null && node.getParent() != null) {
                node = node.getParent();

                Stream.iterate(node, Objects::nonNull, Node::getParent)
                  .forEach(knownParents::add);
              }
            }
          }

          int orphaned = 0;
          int attached = 0;

          Map<Node, Set<Node>> children = new HashMap<>();

          for(Map.Entry<Node, Integer> entry : referenceCounters.entrySet()) {
            if(entry.getValue() == 0) {
              orphaned++;

              Node node = entry.getKey();

              Stream.iterate(node, Objects::nonNull, Node::getParent)
                .forEach(n -> children.computeIfAbsent(n.getParent() == null ? null : n.getParent(), k -> new HashSet<>()).add(n));

//              if(!knownParents.contains(node)) {  // filter out duplicates
//                System.out.println("--- Not GC'd: " +
//                  Stream.iterate(node, Objects::nonNull, Node::getParent)
//                    .map(n -> (referenceCounters.getOrDefault(n, 0) > 0 ? "[R] " : "") + n.toString())
//                    .collect(Collectors.joining("\n       +-- "))
//                );
//              }
            }
            else {
              attached++;
            }
          }

          if(orphaned > 0) {
            System.out.println("Retained Object Graph: " + children.get(null));

            recurse(children, children.get(null), "   ");

            System.out.println("--- Nodes attached/orphaned " + attached + "/" + orphaned);
          }
        }
      }
    }
    catch(InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  private final void recurse(Map<Node, Set<Node>> childMap, Set<Node> set, String level) {
    List<Node> list = new ArrayList<>(set);

    for(int i = 0; i < list.size(); i++) {
      Node node = list.get(i);
      System.out.println(level + "+-- " + (referenceCounters.getOrDefault(node, 0) > 0 ? "[R] " : "") + node.toString());

      Set<Node> childSet = childMap.get(node);

      if(childSet != null) {
        recurse(childMap, childSet, level + (i == list.size() - 1 ? "    " : "|   "));
      }
    }
  }

  private static final class FakeAddChange extends Change<Node> {
    private boolean first = true;

    private FakeAddChange(Parent current) {
      super(current.getChildrenUnmodifiable());
    }

    @Override
    public boolean next() {
      boolean next = first;
      first = false;
      return next;
    }

    @Override
    public void reset() {
      first = true;
    }

    @Override
    public boolean wasAdded() {
      return true;
    }

    @Override
    public int getFrom() {
      return 0;
    }

    @Override
    public int getTo() {
      return getList().size();
    }

    @Override
    public List<Node> getRemoved() {
      return List.of();
    }

    @Override
    protected int[] getPermutation() {
      return null;
    }
  }

  private static final class FakeRemoveChange extends Change<Node> {
    private boolean first = true;

    private FakeRemoveChange(Parent current) {
      super(current.getChildrenUnmodifiable());
    }

    @Override
    public boolean next() {
      boolean next = first;
      first = false;
      return next;
    }

    @Override
    public void reset() {
      first = true;
    }

    @Override
    public boolean wasAdded() {
      return false;
    }

    @Override
    public boolean wasRemoved() {
      return true;
    }

    @Override
    public int getFrom() {
      return 0;
    }

    @Override
    public int getTo() {
      return 0;
    }

    @Override
    public List<Node> getRemoved() {
      return getList();
    }

    @Override
    protected int[] getPermutation() {
      return null;
    }
  }
}
