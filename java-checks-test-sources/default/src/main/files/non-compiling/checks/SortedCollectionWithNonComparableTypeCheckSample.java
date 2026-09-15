package checks;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

class SortedCollectionWithNonComparableTypeCheckSample {

  static class Task {
  }

  static class UnknownHierarchy extends UnknownParent {
  }

  void incompleteSemantic(Comparator<Task> comparator) {
    Set<Task> detected = new TreeSet<>(); // Noncompliant
    Set<UnknownType> unknownType = new TreeSet<UnknownType>();
    Set<UnknownHierarchy> unknownHierarchy = new TreeSet<UnknownHierarchy>();
    Set<Task> unresolvedComparator = new TreeSet<>(missingComparator);
    Set<Task> resolvedComparator = new TreeSet<>(comparator);
  }
}
