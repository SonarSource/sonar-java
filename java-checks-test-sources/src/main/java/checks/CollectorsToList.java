package checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectorsToList {
  void noncompliant() {
    List<String> list1 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Noncompliant [[sc=16;ec=35]] {{Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()'}}

    List<String> list2 = Stream.of("A", "B", "C")
      .collect(Collectors.toUnmodifiableList()); // Noncompliant [[sc=16;ec=47]] {{Replace this usage of 'Stream.collect(Collectors.toUnmodifiableList())' with 'Stream.toList()'}}

    Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Noncompliant [[sc=16;ec=35]] {{Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()'}}

    Stream.of("A", "B", "C")
      .collect(Collectors.toUnmodifiableList()); // Noncompliant [[sc=16;ec=47]] {{Replace this usage of 'Stream.collect(Collectors.toUnmodifiableList())' with 'Stream.toList()'}}
  }


  private List<String> memberList;

  void compliant() {
    List<String> list1 = Stream.of("A", "B", "C").toList(); // Compliant

    List<String> list2 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant, list2 needs to be mutable

    list2.add("X");

    memberList = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant, memberList needs to be mutable as its modified in addX

    List<String> list3 = Stream.of("A", "B", "C")
      .collect(Collectors.toCollection(ArrayList::new)); // Compliant because it's creating a specific list type instead of using toList
  }

  void addX() {
    memberList.add("X");
  }

  void FNs() {
    Collector<String, ?, List<String>> collector = Collectors.toUnmodifiableList();
    List<String> list1 = Stream.of("A", "B", "C").collect(collector); // FN because we don't track the collector through variables
  }

  private List<String> memberList2;
  List<String>[] arr;

  List<String> FPs() {
    List<String> list1 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Noncompliant - FP because we don't track lists across methods
    addX(list1);

     arr[0] = Stream.of("A", "B", "C")
       .collect(Collectors.toList()); // Noncompliant - FP we don't detect the modification through the array
    arr[0].add("X");

    addX(Stream.of("A", "B", "C").collect(Collectors.toList())); // Noncompliant - same reason

    memberList2 = Stream.of("A", "B", "C").collect(Collectors.toList()); // Noncompliant - FP, see addX2

    return Stream.of("A", "B", "C").collect(Collectors.toList()); // Noncompliant - FP because we don't check how the return value is used
  }

  void useFPs() {
    FPs().add("X");
  }

  void addX(List<String> string) {
    memberList.add("X");
  }

  void addX2() {
    getMemberList2().add("X"); // We don't detect this modification on memberList2 because we don't follow through the getter
  }

  List<String> getMemberList2() {
    return memberList2;
  }

  // Test that we don't throw an exception when List.add is called without a receiver
  private static class MyList extends ArrayList<String> {
    void addX() {
      add("X");
    }
  }
}
