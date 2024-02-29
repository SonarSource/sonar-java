package checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
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
      .collect(Collectors.toList()); // Noncompliant [[sc=16;ec=35]] {{Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()'}}
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
      .collect(Collectors.toList()); // Noncompliant, below methods do not modify the list
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

  List<String> fixedFPs() {
    List<String> list1 = Stream.of("A", "B", "C")
      .collect(Collectors.toList()); // Compliant
    addX(list1);

    addX(Stream.of("A", "B", "C").collect(Collectors.toList())); // Compliant

    memberList2 = Stream.of("A", "B", "C").collect(Collectors.toList()); // Compliant

    return Stream.of("A", "B", "C").collect(Collectors.toList()); // Compliant
  }

  void useFixedFPs() {
    fixedFPs().add("X");
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

  private Collection<CharSequence> upcast(Stream<String> stream) {
    return stream.collect(Collectors.toList()); // Compliant
  }

  private Collection<CharSequence> noUpcast(Stream<CharSequence> stream) {
    return stream.collect(Collectors.toList()); // Noncompliant
  }

  private Collection<CharSequence> upcastInlineStream() {
    return Stream.of(1, 2)
      .map(String::valueOf)
      .collect(Collectors.toList()); // Compliant
  }

  private Collection rawReceiver() {
    return Stream.of(1, 2)
      .map(String::valueOf)
      .collect(Collectors.toList()); // Noncompliant
  }

  private Object rawReceiverAndArgument(Stream stream) {
    return stream.collect(Collectors.toList()); // Noncompliant
  }

  public Object returnFromPublic1(Stream stream) {
    return stream.collect(Collectors.toList()); // Compliant
  }

  public Object returnFromPublic2(Stream<String> stream) {
    return stream.collect(Collectors.toUnmodifiableList()); // Noncompliant
  }

  public Object returnFromPublic3(Stream<String> stream) {
    return (Object) stream.collect(Collectors.toList()); // Compliant
  }

  private Object returnFromPublic4(Stream<String> stream) {
    return (Object) stream.collect(Collectors.toList()); // Compliant; FN due to simplified data flow analysis
  }

  Object returnFromPackageProtected(Stream stream) {
    return stream.collect(Collectors.toList()); // Compliant
  }

  private void lambda1(Stream<String> stream) {
    Supplier<List<String>> x = () -> stream.collect(Collectors.toList()); // Compliant
  }

  private List<String> lambda2(Stream<String> stream) {
    Supplier<List<String>> x = () -> stream.collect(Collectors.toList()); // Compliant
    return x.get();
  }

  public List<String> lambda3(Stream<String> stream) {
    Supplier<List<String>> x = () -> stream.collect(Collectors.toList()); // Compliant
    return x.get();
  }

  private void lambda4(Stream<String> stream) {
    Supplier<List<String>> x = () -> {
      var list = stream.collect(Collectors.toList()); // Noncompliant
      return null;
    };
  }

  private void lambda5(Stream<String> stream) {
    Supplier<List<String>> x = () -> {
      var list = stream.collect(Collectors.toList()); // Compliant
      return list;
    };
  }

  private List<String> yield1(int code, Stream<String> stream) {
    switch (code) {
      case 0 -> {
        return List.of("Hello", "world");
      }
      case 1 -> {
        return stream.collect(Collectors.toList()); // Noncompliant
      }
      default -> {
        return null;
      }
    }
  }

  private List<String> yield2(int code, Stream<String> stream) {
    return switch (code) {
      case 0 -> List.of("Hello", "world");
      case 1 -> stream.collect(Collectors.toList()); // Noncompliant
      default -> null;
    };
  }

  public List<String> yield3(int code, Stream<String> stream) {
    var x = switch (code) {
      case 0 -> List.of("Hello", "world");
      case 1 -> {
        System.out.println();
        yield stream.collect(Collectors.toList()); // Noncompliant
      }
      default -> null;
    };
    return null;
  }

  public List<String> yield4(int code, Stream<String> stream) {
    var x = switch (code) {
      case 0 -> List.of("Hello", "world");
      case 1 -> {
        System.out.println();
        yield stream.collect(Collectors.toList()); // Compliant
      }
      default -> null;
    };
    return x;
  }

  public List<String> yield5(int code, Stream<String> stream) {
    var x = switch (code) {
      case 0 -> List.of("Hello", "world");
      case 1 -> {
        var y = stream.collect(Collectors.toList()); // Noncompliant
        System.out.println();
        yield y;
      }
      default -> null;
    };
    return null;
  }

  public List<String> graylist1(int code, Stream<String> stream) {
    List<String> z = null;
    var x = switch (code) {
      case 0 -> List.of("Hello", "world");
      case 1 -> {
        var y = stream.collect(Collectors.toList()); // Noncompliant
        System.out.println();
        yield y;
      }
      default -> null;
    };
    return z;
  }

  public List<String> graylist2(int code, Stream<String> stream) {
    List<String> z = null;
    var x = switch (code) {
      case 0 -> List.of("Hello", "world");
      case 1 -> {
        var y = stream.collect(Collectors.toList()); // Compliant
        System.out.println();
        yield y;
      }
      default -> null;
    };
    z = x;
    return z;
  }

  private Object graylist3(int code, Stream<String> stream) {
    List<String> z = null;
    var x = switch (code) {
      case 0 -> List.of("Hello", "world");
      case 1 -> {
        var y = stream.collect(Collectors.toList()); // Noncompliant
        System.out.println();
        yield y;
      }
      default -> null;
    };
    z = x;
    Object a = z;
    return a;
  }

  public Object graylist4(int code, Stream<String> stream) {
    List<String> z = null;
    var x = switch (code) {
      case 0 -> List.of("Hello", "world");
      case 1 -> {
        var y = stream.collect(Collectors.toList()); // Compliant
        System.out.println();
        yield y;
      }
      default -> null;
    };
    z = x;
    Object a = z;
    return a;
  }

  public Object graylist5(int code, Stream<String> stream) {
    List<String> z = null;
    var x = switch (code) {
      case 0 -> List.of("Hello", "world");
      case 1 -> {
        var y = stream.collect(Collectors.toList()); // Compliant; FN due to simplified data flow analysis
        System.out.println();
        yield y;
      }
      default -> null;
    };
    Object a = z;
    z = x;
    return a;
  }

  private void assignment(Stream<String> stream) {
    var a0 = stream.collect(Collectors.toList()); // Noncompliant
    var a1 = stream.collect(Collectors.toList()); // Compliant
    var a2 = stream.collect(Collectors.toList()); // Noncompliant

    var b = new Object[10];
    b[1] = stream.collect(Collectors.toList()); // Compliant
    b[2] = a1;
    var c = a2;
  }

  private void callTarget(Stream<String> stream) {

    var d0 = stream.collect(Collectors.toList()); // Compliant
    var e0 = d0;
    var f0 = e0;
    var g0 = f0;
    g0.add("foo");

    var d1 = stream.collect(Collectors.toList()); // Noncompliant
    var e1 = d1;
    var f1 = e1;
    var g1 = f1;
    g0.contains("foo");
  }

  private Object argument0(Stream<String> stream) {
    return foo(stream.collect(Collectors.toList())); // Compliant
  }

  private Object argument1(Stream<String> stream) {
    return bar(stream.collect(Collectors.toList())); // Compliant; FN due to simplified data flow analysis
  }

  private void argument2(Stream<String> stream) {
    Collections.reverse(stream.collect(Collectors.toList())); // Compliant
  }

  private void argument3(Stream<String> stream) {
    Collections.max(stream.collect(Collectors.toList())); // Noncompliant
  }

  private void standalone(Stream<String> stream) {
    stream.collect(Collectors.toList()); // Noncompliant
  }

  private Object foo(List<String> list) {
    return list;
  }

  private Object bar(List<String> list) {
    return null;
  }

  private void field(Stream<String> stream) {
    var localList = stream.collect(Collectors.toList()); // Noncompliant
    nonLocalList = stream.collect(Collectors.toList()); // Compliant
    staticNonLocalList = stream.collect(Collectors.toList()); // Compliant
    this.nonLocalList = stream.collect(Collectors.toList()); // Compliant
    CollectorsToListCheckSample.staticNonLocalList = stream.collect(Collectors.toList()); // Compliant
  }

  private Object nonLocalList;

  private static Object staticNonLocalList;

  public static void sonarjava4422Sample1()
  {
    final List<String> test = sonarjava4422Sample2();
    test.add("C"); // Here we NEED the list to be modifiable
  }

  public static List<String> sonarjava4422Sample2()
  {
    return Stream.of("A", "B").collect(Collectors.toList()); // Compliant
  }
}
