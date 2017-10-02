import java.util.stream.*;
import java.util.List;

abstract class A {

  void test() {
    Stream<Integer> stream = Stream.of(1, 2, 3);
    stream.count(); // flow@stream {{Pipeline is consumed here.}}
    stream.findAny(); // Noncompliant [[flows=stream]] {{Refactor this code so that this consumed stream pipeline is not reused.}}
  }

  void intStream() {
    IntStream range = IntStream.range(0, 10);
    range.count(); // flow@is {{Pipeline is consumed here.}}
    range.average(); // Noncompliant [[flows=is]]
    range.filter(i -> true); // Doesn't raise issue, because exception is thrown on previous line and SE is stopped
  }

  void pipeline() {
    IntStream range = IntStream.range(0, 10);
    range
      .filter(i -> i % 2 == 0)
      .count(); // flow@pipe {{Pipeline is consumed here.}}
    range.average(); // Noncompliant [[flows=pipe]]
  }

  void pipeline2() {
    IntStream range = IntStream.range(0, 10);
    IntStream filtered = range.filter(i -> true);
    range.count();
    filtered.count(); // Noncompliant
  }

  void pipeline3(boolean test) {
    IntStream range = IntStream.range(0, 10);
    IntStream filtered = range.filter(i -> true);
    if (test) {
      range.count();
    } else {
      filtered.count();
    }
  }

  void cond(boolean test, boolean other) {
    IntStream range = IntStream.range(0, 10);
    IntStream filtered = range.filter(i -> true);
    if (test) {
      range.count(); // flow@cond
    }
    if (other) {
      filtered.count(); // Noncompliant [[flows=cond]]
    }
  }

  boolean useMethodReference1(List<Boolean> list) {
    Stream<Boolean> filter = list.stream().filter(Boolean::booleanValue);
    java.util.Iterator<Boolean> itr = filter.iterator();
    return consumes(filter::iterator); // Noncompliant
  }
  boolean useMethodReference2(List<Boolean> list) {
    Stream<Boolean> filter = list.stream().filter(Boolean::booleanValue);
    return consumes(filter::iterator); // Compliant
  }
  boolean useMethodReference3(List<Stream<Boolean>> list) {
    return list.stream().map(Stream::iterator).count() > 0; // Compliant
  }
  abstract boolean consumes(Iterable<Boolean> iterable);

  List list;

  void test() {
    list.stream().count();
    list.stream().filter(e -> true).collect(Collectors.toList());
  }

}
