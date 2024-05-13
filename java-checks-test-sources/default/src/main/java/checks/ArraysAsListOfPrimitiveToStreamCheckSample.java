package checks;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

class ArraysAsListOfPrimitiveToStreamCheckSample {
  void foo(Set<String> s) {
    Arrays.asList("a1", "a2", "b1", "c2", "c1").stream().close(); // Compliant
    Arrays.asList(1.0, 2, 3L).stream().close(); // Compliant

    Arrays.asList(1, 2, 3, 4).stream().close(); // Noncompliant {{Use "Arrays.stream" instead of "Arrays.asList".}}
//  ^^^^^^^^^^^^^
    Arrays.asList(1L, 2L, 3L, 4L).stream().close(); // Noncompliant {{Use "Arrays.stream" instead of "Arrays.asList".}}
//  ^^^^^^^^^^^^^
    Arrays.asList(1.0, 2.0, 3.0, 4.0).stream().close(); // Noncompliant {{Use "Arrays.stream" instead of "Arrays.asList".}}
//  ^^^^^^^^^^^^^

    List<Integer> integers = Arrays.asList(1, 2, 3, 4);
    integers.stream().close(); // Compliant - check only target inlined constructions
    getList().stream().close();
  }

  List<Integer> getList() { return null; }
}
