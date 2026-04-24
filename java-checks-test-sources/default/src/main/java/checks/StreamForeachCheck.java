package checks;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class StreamForeachCheck {

  void unnecessaryStreamForEach(Collection<Integer> collection) {
    collection.stream().forEach(System.out::println); // Noncompliant {{Simplify the code by replacing .stream().forEach() with .forEach().}}
    //         ^^^^^^^^^^^^^^^^
  }

  void compliantCollectionForEach(Collection<Integer> collection) {
    collection.forEach(System.out::println); // Compliant
  }

  void necessaryFilterForEach(Collection<String> col) {
    col.stream().filter(s -> !s.isEmpty()).forEach(System.out::println);
  }

  void unnecessaryForEachOnSet(Set<String> set) {
    set.stream().forEach(e -> System.out.println("Element: " + e)); // Noncompliant
  }

  void unnecessaryForEachOnList(List<String> list) {
    list.stream().forEach(e -> System.out.println("Element: " + e)); // Noncompliant
  }

  void necessaryForEachOnParallelStream(Set<String> set) {
    set.parallelStream().forEach(System.out::println); // Compliant
  }

  void sequentialStreamForEach(Collection<String> col) {
    Stream<String> s = col.stream();
    s.forEach(System.out::println); // Compliant (the rule ignores this case)
  }

}
