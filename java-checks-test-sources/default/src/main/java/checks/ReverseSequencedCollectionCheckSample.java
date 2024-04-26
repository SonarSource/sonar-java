package checks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

class ReverseSequencedCollectionCheckSample {

  List<String> field;

  List<String> test_read_only_usage_as_argument(List<String> source) {
    var list = new ArrayList<>(source);
    Collections.reverse(list); // Noncompliant
    var list2 = new ArrayList<>(list);
    list2.addAll(list);
    return List.copyOf(list);
  }

  void test_noncompliant_for(List<String> source) {
    var list = new ArrayList<>(source);
    list.add("a");
    Collections.reverse(list); // Noncompliant {{Remove this "reverse" statement and replace "list" with "list.reversed()" after.}}
    for (var e : list) {
      System.out.println(e);
    }
  }

  void test_external_list() {
    var list = externalList();
    Collections.reverse(list); // Compliant, side effect on externalList()
    list.forEach(System.out::println);
  }

  void test_unknown_usage_before(List<String> source) {
    var list = new ArrayList<>(source);
    unknownUsage(list);
    Collections.reverse(list); // Compliant, unknown usage before
    list.forEach(System.out::println);
  }

  void test_unknown_usage_after(List<String> source) {
    var list = new ArrayList<>(source);
    Collections.reverse(list); // Compliant, unknown usage after
    unknownUsage(list);
    list.forEach(System.out::println);
  }

  void test_non_supported_constructor(List<String> source) {
    var list = new MyList(source);
    Collections.reverse(list); // Compliant, not supported constructor MyList
    list.forEach(System.out::println);
  }

  void test_write_after(List<String> source) {
    var list = new ArrayList<>(source);
    Collections.reverse(list); // Compliant, write after
    list.add("a");
    list.forEach(System.out::println);
  }

  void test_compliant_field(List<String> source) {
    field = new ArrayList<>(source);
    Collections.reverse(field); // Compliant, field could be used outside of this method
    field.forEach(System.out::println);
  }

  void test_argument_and_reverse(List<String> list) {
    Collections.reverse(list); // Compliant, the caller needs the "list" argument to be reversed
  }

  void test_argument_and_for(List<String> list) {
    Collections.reverse(list); // Compliant, we don't know if the caller needs the "list" argument to be reversed
    for (var e : list) {
      System.out.println(e);
    }
  }


  List<String> test_return_list(List<String> source) {
    var list = new ArrayList<>(source);
    Collections.reverse(list); // Compliant, we don't know if the caller can work with the "reversed()" view of the list
    return list;
  }

  List<String> test_non_supported_initializer(List<String> source) {
    var list = new ArrayList<>(source);
    var spy = list;
    Collections.reverse(list); // Compliant, list is assigned to another variable
    return spy;
  }

  List<String> test_non_supported_assigment(List<String> source) {
    var list = new ArrayList<>(source);
    List<String> spy;
    spy = list;
    Collections.reverse(list); // Compliant, list is assigned to another variable
    return spy;
  }

  String[] test_return_array(List<String> source) {
    var list = new ArrayList<>(source);
    list.add("A");
    if (!list.isEmpty()) { // not a loop
      Collections.reverse(list); // Noncompliant
    }
    return list.toArray(String[]::new);
  }

  void test_noncompliant_fori_loop(List<String> source, int count) {
    var list = new ArrayList<>(source);
    list.add("A");
    for (int i = 0; i < count; i++) {
      Collections.reverse(list); // Compliant, reverse is in a loop
    }
    list.forEach(System.out::println);
  }

  void test_noncompliant_while_loop(List<String> source, int count) {
    List<String> list;
    list = new ArrayList<>(source);
    while(--count >= 0) {
      Collections.reverse(list); // Compliant, reverse is in a loop
    }
    list.forEach(System.out::println);
  }

  void test_compliant_usage(List<String> list) {
    for (var e : list.reversed()) {
      System.out.println(e);
    }
  }

  void test_noncompliant_stream(List<String> source) {
    var copy = new ArrayList<>(source);
    copy.add("a");
    Collections.reverse(copy); // Noncompliant {{Remove this "reverse" statement and replace "copy" with "copy.reversed()" after.}}
    copy.forEach(System.out::println);
    copy.stream().forEach(System.out::println);
  }

  Object[] test_no_initializer(List<String> source) {
    List<String> list;
    if (source.isEmpty()) {
      list = new ArrayList<>();
    } else {
      list = new ArrayList<>(source);
    }
    list.add("A");
    if (!list.isEmpty()) {
      Collections.reverse(list); // Noncompliant
    }
    return list.toArray();
  }

  void test_null_initializer(List<String> source) {
    List<String> list = null;
    if (source.isEmpty()) {
      list = null;
    } else if (!source.isEmpty()) {
      list = new ArrayList<>(source);
    }
    list.add("A");
    if (!source.isEmpty()) {
      Collections.reverse(list); // Noncompliant
      list.forEach(System.out::println);
    }
  }

  void test_reverse_non_identifier(List<String> source) {
    List<String> list = new ArrayList<>(source);
    Collections.reverse(list.subList(0,3)); // Compliant, partial reverse
    list.forEach(System.out::println);
  }

  void test_reverse_foreach_initialization(List<List<String>> sources) {
    for (List<String> list : sources) {
      Collections.reverse(list);// Compliant, foreach initialization is not supported
      list.forEach(System.out::println);
    }
  }

  List<String> externalList() {
    return new ArrayList<>();
  }
  void unknownUsage(List<String> list) {
  }

  class MyList extends ArrayList<String> {
    public MyList(@NotNull Collection<? extends String> c) {
      super(c);
    }
  }

}
