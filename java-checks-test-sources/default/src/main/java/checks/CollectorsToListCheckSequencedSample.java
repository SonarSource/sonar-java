package checks;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectorsToListCheckSequencedSample {

  void noncompliant() {
    List<String> list = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Noncompliant {{Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.}}
//             ^^^^^^^^^^^^^^^^^^^

    list.getFirst();
  }

  void compliant() {
    List<String> addFirst = Stream.of("B", "C", "D")
      .collect(Collectors.toList()); // Compliant, addFirst needs to be mutable

    addFirst.addFirst("A");

    List<String> addLast = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant, addLast needs to be mutable

    addLast.addLast("D");

    List<String> removeFirst = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant, removeFirst needs to be mutable

    removeFirst.removeFirst();

    List<String> removeLast = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant, removeLast needs to be mutable

    removeLast.removeLast();
  }
}
