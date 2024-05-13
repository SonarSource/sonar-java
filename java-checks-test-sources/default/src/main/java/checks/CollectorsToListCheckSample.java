package checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectorsToListCheckSample {
  static class ListWrapper {
    List<String> strings;
  }

  ListWrapper listWrapper = new ListWrapper();

  void noncompliant() {
    List<String> list1 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Noncompliant {{Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.}}
//             ^^^^^^^^^^^^^^^^^^^

    // Not modifying the list
    list1.contains("B");

    List<String> list2 = Stream.of("A", "B", "C")
      .collect(Collectors.toUnmodifiableList()); // Noncompliant {{Replace this usage of 'Stream.collect(Collectors.toUnmodifiableList())' with 'Stream.toList()'.}}
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Noncompliant {{Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.}}
//             ^^^^^^^^^^^^^^^^^^^

    Stream.of("A", "B", "C")
      .collect(Collectors.toUnmodifiableList()); // Noncompliant {{Replace this usage of 'Stream.collect(Collectors.toUnmodifiableList())' with 'Stream.toList()'.}}
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    List<List<String>> listOfLists = new ArrayList<>();
    // list1 appears in a call to List.add, but it is not the receiver, so it should not be interpreted as mutable:
    listOfLists.add(list1);

    listWrapper.strings = Stream.of("A", "B", "C").collect(Collectors.toList());
    // listWrapper.strings appears in a call to List.add, but it is not the receiver, so it should not be interpreted as mutable:
    listOfLists.add(listWrapper.strings);
  }

  void compliant_collections_methods() {
    var myMutableList = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant
    Collections.shuffle(myMutableList);

    var myMutableList2 = Stream.of("A", "B")
      .collect(Collectors.toList()); // Compliant
    Collections.rotate(myMutableList2, 2);

    var myMutableList3 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant
    Collections.addAll(myMutableList3, "D", "E");

    var myMutableList4 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant
    Collections.replaceAll(myMutableList4, "B", "D");

    var myMutableList5 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant
    Collections.reverse(myMutableList5);
  }

  void noncompliant_collections_methods() {
    var myMutableList = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Noncompliant
    Collections.max(myMutableList);
    Collections.frequency(myMutableList, "A");
    Collections.synchronizedList(myMutableList);
    Collections.unmodifiableList(myMutableList);
  }

  private List<String> memberList;
  private List<String> memberListAccessedWithThis;
  List<String>[] arr;
  ListWrapper listWrapper2 = new ListWrapper();


  void compliant() {
    List<String> list1 = Stream.of("A", "B", "C").toList(); // Compliant

    List<String> list2 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant, list2 needs to be mutable

    list2.add("X");

    List<String> list3 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant, list3 needs to be mutable

    list3.retainAll(Arrays.asList("C", "D"));

    memberList = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant, memberList needs to be mutable as its modified in addX

    this.memberListAccessedWithThis = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant, memberListAccessedWithThis needs to be mutable as its modified in addX

    arr[0] = Stream.of("A", "B", "C").collect(Collectors.toList()); // Compliant, list is modified in addX

    listWrapper2.strings = Stream.of("A", "B", "C").collect(Collectors.toList()); // Compliant, list is modified in addX

    List<String> list4 = Stream.of("A", "B", "C")
      .collect(Collectors.toCollection(ArrayList::new)); // Compliant because it's creating a specific list type instead of using toList

    List<String> list5 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant, list5 needs to be mutable

    list5.removeIf(s -> true);
  }

  void addX() {
    memberList.add("X");
    this.memberListAccessedWithThis.add("X");
    arr[0].add("X");
    listWrapper2.strings.add("X");
  }

  void FNs() {
    Collector<String, ?, List<String>> collector = Collectors.toUnmodifiableList();
    List<String> list1 = Stream.of("A", "B", "C").collect(collector); // FN because we don't track the collector through variables
  }

  private List<String> memberList2;

  List<String> FPs() {
    List<String> list1 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Noncompliant
    addX(list1);

    addX(Stream.of("A", "B", "C").collect(Collectors.toList())); // Noncompliant

    memberList2 = Stream.of("A", "B", "C").collect(Collectors.toList()); // Noncompliant

    return Stream.of("A", "B", "C").collect(Collectors.toList()); // Noncompliant
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

  Collection<CharSequence> upcast(Stream<String> stream) {
    return stream.collect(Collectors.toList()); // Compliant
  }

  Collection<CharSequence> noUpcast(Stream<CharSequence> stream) {
    return stream.collect(Collectors.toList()); // Noncompliant
  }

  Collection<CharSequence> upcastInlineStream() {
    return Stream.of(1, 2)
      .map(String::valueOf)
      .collect(Collectors.toList()); // Compliant
  }

  Collection rawReceiver() {
    return Stream.of(1, 2)
      .map(String::valueOf)
      .collect(Collectors.toList()); // Noncompliant
  }

  Object rawReceiverAndArgument(Stream stream) {
    return stream.collect(Collectors.toList()); // Noncompliant
  }
}
