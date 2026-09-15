package checks;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

class SortedCollectionWithNonComparableTypeCheckSample {

  static class Task {
    int priority;
  }

  static class ComparableTask implements Comparable<ComparableTask> {
    @Override
    public int compareTo(ComparableTask other) {
      return 0;
    }
  }

  void noncompliant(Collection<Task> tasks, Map<Task, String> assignments) {
    Set<Task> treeSet = new TreeSet<>(); // Noncompliant {{Provide a comparator because this element or key type does not implement "Comparable".}}
    Map<Task, String> treeMap = new TreeMap<>(); // Noncompliant
    Queue<Task> priorityQueue = new PriorityQueue<>(); // Noncompliant
    Queue<Task> priorityQueueWithCapacity = new PriorityQueue<>(10); // Noncompliant
    Set<Task> skipListSet = new ConcurrentSkipListSet<>(); // Noncompliant
    Map<Task, String> skipListMap = new ConcurrentSkipListMap<>(); // Noncompliant
    Set<Task> treeSetFromCollection = new TreeSet<>(tasks); // Noncompliant
    Map<Task, String> treeMapFromMap = new TreeMap<>(assignments); // Noncompliant
    Queue<Task> priorityQueueFromCollection = new PriorityQueue<>(tasks); // Noncompliant
    Set<Task> skipListSetFromCollection = new ConcurrentSkipListSet<>(tasks); // Noncompliant
    Map<Task, String> skipListMapFromMap = new ConcurrentSkipListMap<>(assignments); // Noncompliant
  }

  void compliant(
    Comparator<Task> comparator,
    SortedSet<Task> sortedSet,
    SortedMap<Task, String> sortedMap,
    PriorityQueue<Task> orderedQueue) {
    Set<Task> treeSet = new TreeSet<>(comparator);
    Map<Task, String> treeMap = new TreeMap<>(comparator);
    Queue<Task> priorityQueue = new PriorityQueue<>(comparator);
    Queue<Task> priorityQueueWithCapacity = new PriorityQueue<>(10, comparator);
    Set<Task> skipListSet = new ConcurrentSkipListSet<>(comparator);
    Map<Task, String> skipListMap = new ConcurrentSkipListMap<>(comparator);
    Set<Task> copiedTreeSet = new TreeSet<>(sortedSet);
    Map<Task, String> copiedTreeMap = new TreeMap<>(sortedMap);
    Queue<Task> copiedQueue = new PriorityQueue<>(orderedQueue);
    Set<Task> copiedSkipListSet = new ConcurrentSkipListSet<>(sortedSet);
    Map<Task, String> copiedSkipListMap = new ConcurrentSkipListMap<>(sortedMap);

    Set<ComparableTask> comparableTreeSet = new TreeSet<>();
    Map<ComparableTask, String> comparableTreeMap = new TreeMap<>();
    Queue<ComparableTask> comparableQueue = new PriorityQueue<>();

    TreeSet rawTreeSet = new TreeSet();
  }

  <T> void generic() {
    Set<T> values = new TreeSet<>();
  }
}
