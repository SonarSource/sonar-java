package checks;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
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

  Set<Task> escaped;

  static class Task {
    int priority;
  }

  static class ComparableTask implements Comparable<ComparableTask> {
    @Override
    public int compareTo(ComparableTask other) {
      return 0;
    }
  }

  static class BroadlyComparableTask implements Comparable<Object> {
    @Override
    public int compareTo(Object other) {
      return 0;
    }
  }

  static class IncompatiblyComparableTask implements Comparable<String> {
    @Override
    public int compareTo(String other) {
      return 0;
    }
  }

  static class RawComparableTask implements Comparable {
    @Override
    public int compareTo(Object other) {
      return 0;
    }
  }

  static class ComparableTaskChild extends ComparableTask {
  }

  interface NonComparableContract {
  }

  abstract static class AbstractTask {
  }

  void noncompliant(Collection<Task> tasks, Map<Task, String> assignments, PriorityQueue<Task> orderedTasks) {
    Set<Task> treeSet = new TreeSet<>(); // Noncompliant {{Provide a comparator because this element type does not implement "Comparable".}}
    Map<Task, String> treeMap = new TreeMap<>(); // Noncompliant {{Provide a comparator because this key type does not implement "Comparable".}}
    Queue<Task> priorityQueue = new PriorityQueue<>(); // Noncompliant
    Queue<Task> priorityQueueWithCapacity = new PriorityQueue<>(10); // Noncompliant
    Set<Task> skipListSet = new ConcurrentSkipListSet<>(); // Noncompliant
    Map<Task, String> skipListMap = new ConcurrentSkipListMap<>(); // Noncompliant
    Set<Task> treeSetFromCollection = new TreeSet<>(tasks); // Noncompliant
    Map<Task, String> treeMapFromMap = new TreeMap<>(assignments); // Noncompliant
    Queue<Task> priorityQueueFromCollection = new PriorityQueue<>(tasks); // Noncompliant
    Set<Task> skipListSetFromCollection = new ConcurrentSkipListSet<>(tasks); // Noncompliant
    Map<Task, String> skipListMapFromMap = new ConcurrentSkipListMap<>(assignments); // Noncompliant
    Set<Task> treeSetFromPriorityQueue = new TreeSet<>(orderedTasks); // Noncompliant
    Set<IncompatiblyComparableTask> incompatibleComparableTreeSet = new TreeSet<>(); // Noncompliant
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
    Set<BroadlyComparableTask> broadlyComparableTreeSet = new TreeSet<>();
    Set<RawComparableTask> rawComparableTreeSet = new TreeSet<>();
    Set<Comparable<Object>> comparableInterfaceTreeSet = new TreeSet<>();

    Set<Object> objectTreeSet = new TreeSet<>();
    Map<Object, Object> objectTreeMap = new TreeMap<>();
    Queue<Object> objectPriorityQueue = new PriorityQueue<>();
    Set<Object> objectSkipListSet = new ConcurrentSkipListSet<>();
    Map<Object, Object> objectSkipListMap = new ConcurrentSkipListMap<>();
    Set<NonComparableContract> interfaceTreeSet = new TreeSet<>();
    Set<AbstractTask> abstractTreeSet = new TreeSet<>();
    Set<?> wildcardTreeSet = new TreeSet<>();
    Set<Object> emptyObjectTreeSet = new TreeSet<>(Collections.emptyList());
    SortedMap<Object, Object> emptyUnmodifiableTreeMap = Collections.unmodifiableSortedMap(new TreeMap<>());

    Map<String, Object> stringKeys = new TreeMap<>();
    Map<Integer, Object> integerKeys = new TreeMap<>();
    Map<Long, Object> longKeys = new TreeMap<>();
    Map<Character, Object> characterKeys = new TreeMap<>();
    Map<Path, Object> pathKeys = new TreeMap<>();
    Map<ComparableTaskChild, Object> inheritedComparableKeys = new TreeMap<>();
    TreeMap<String, Object> explicitStringKeys = new TreeMap<String, Object>();
    Map<String, Object> parenthesizedStringKeys = (new TreeMap<>());

    TreeSet rawTreeSet = new TreeSet();
    TreeMap rawTreeMap = new TreeMap();
    PriorityQueue rawPriorityQueue = new PriorityQueue();
  }

  Map<Object, Object> objectMapFactory() {
    return new TreeMap<>();
  }

  void handledComparisonFailure(Collection<Task> tasks) {
    Set<Task> unhandled = new TreeSet<>(tasks); // Noncompliant
    Set<Task> outer;
    try {
      Set<Task> handled = new TreeSet<>(tasks);
    } catch (ClassCastException e) {
    }
    try {
      Set<Task> handledByMultiCatch = new TreeSet<>(tasks);
    } catch (ClassCastException | IllegalArgumentException e) {
    }
    try {
      Set<Task> notHandled = new TreeSet<>(tasks); // Noncompliant
    } catch (IllegalArgumentException e) {
    }
    try {
      Set<Task> noArgConstructor = new TreeSet<>(); // Noncompliant
    } catch (ClassCastException e) {
    }
    try {
      escaped = new TreeSet<>(tasks); // Noncompliant
      outer = new TreeSet<>(tasks); // Noncompliant
      Set<Task> used = new TreeSet<>(tasks); // Noncompliant
      used.clear();
    } catch (ClassCastException e) {
    }
    try {
    } catch (ClassCastException e) {
      Set<Task> inCatch = new TreeSet<>(tasks); // Noncompliant
    } finally {
      Set<Task> inFinally = new TreeSet<>(tasks); // Noncompliant
    }
  }

  <T> void generic() {
    Set<T> values = new TreeSet<>();
  }
}
