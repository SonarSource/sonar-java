import java.util.stream.*;
import java.util.List;

class A {

  void test() {
    Stream<Integer> stream = Stream.of(1, 2, 3);
    stream.filter(e -> true); // Noncompliant
  }

  void test2() {
    Stream<Integer> stream = Stream.of(1, 2, 3);
    stream.filter(e -> true).skip(2).limit(1); // Noncompliant
  }

  void test2() {
    Stream<Integer> stream = Stream.of(1, 2, 3);
    stream = stream.filter(e -> true);  // Noncompliant
    stream = stream.skip(2);
    stream = stream.limit(1);
  }


  void compliant() {
    Stream<Integer> stream = Stream.of(1, 2, 3);
    stream.filter(e -> true).count(); // Compliant - consumed
    Stream<Integer> s = Stream.of(1, 2, 3).filter(e -> true);
    s.count( ); // Compliant - consumed
    Stream.of("1","2","3").mapToLong(Long::valueOf).sum(); // Compliant
    List<Integer> boxedStream = IntStream.range(0, 10).boxed().collect(Collectors.toList()); // Compliant
    Stream.of("1","2","3").sorted().iterator(); // Compliant
    Stream.of("1","2","3").sorted().spliterator(); // Compliant
  }

  void onePath(boolean test) {
    IntStream is = IntStream.range(0, 10);

    if (test) {
      is = is.filter(i -> i % 2 == 0); // Noncompliant
    }
    if (!test) {
      is.forEach(System.out::println);
    }
  }

  void testUnknown() {
    s = Stream.of("a", "b", "c").skip(1).unknown();   // Compliant, unknown method
  }

  void streamPassAsArg() {
    s = Stream.concat(
      IntStream.range(0, 3).filter(i -> true), // Compliant, passed outside of method
      Stream.of(2));
  }

  IntStream returningStream() {
    return IntStream.range(0, 10).filter(i -> i % 2); // Compliant, returned
  }

  Stream<Object> streamField;

  void testField() {
    this.streamField = streamField.filter(e -> true); // Compliant, assigned to field
  }

  void exceptionalPath() {
    try {
      Stream.of(1,2,3)
        .filter(e -> true)
        .count();
    } catch (Exception ex) {

    }
  }

  void exceptionalPath2() {
    try {
      Stream.of(1,2,3).filter(e -> true);  // Noncompliant

      Stream.of(1,2,3)
        .filter(e -> true)
        .skip(skipCount())
        .count();

      Stream.of(1,2,3).skip(skipCount()); // Noncompliant
    } catch (Exception ex) {
      System.out.println("Exception!");
    }
  }

  int skipCount() {
    return 42;
  }
}
