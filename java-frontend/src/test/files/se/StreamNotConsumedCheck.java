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

  void exceptionalPath3(Stream<Object> stream) {
    try {
      stream = stream.skip(skipCount());  // Compliant - we don't want to report pipelines not consumed on exceptional paths
    } catch (Exception ex) {
      return;
    }
    stream.forEach(System.out::println);
  }

  boolean useMethodReference1(List<Boolean> list) {
    Stream<Boolean> filter = list.stream().filter(Boolean::booleanValue); // Compliant
    return consumes(filter::iterator);
  }

  Stream<Boolean> field;
  boolean useMethodReference2(List<Boolean> list) {
    this.field = list.stream().filter(Boolean::booleanValue); // Compliant
    return consumes(this.field::iterator);
  }

  boolean useMethodReference3(List<Boolean> list) {
    Stream<Boolean> filter = list.stream().filter(Boolean::booleanValue); // Compliant
    uses(filter);
    return consumes(filter::iterator);
  }

  boolean useMethodReference4(List<Boolean> list) {
    Stream<Boolean> filter = list.stream().filter(Boolean::booleanValue); // Compliant
    java.util.Iterator<Boolean> itr = filter.iterator();
    return consumes(filter::iterator); // raise an issue on S3959 : consumed 2 times
  }

  abstract void uses(Stream<Boolean> stream);
  abstract boolean consumes(Iterable<Boolean> iterable);

  void FN(Stream<Object> stream) {
    stream.filter(e -> true);  // FN - call to filter() is returning same SV as stream, so SV is set CONSUMED on next line
    stream.count();
  }

  int skipCount() {
    return 42;
  }

  void baseStreamMethods(List<Boolean> list) {
    list.stream().unordered(); // Noncompliant
    list.stream().unordered().collect(Collectors.toList()); // Compliant
    list.stream().parallel(); // Noncompliant
    list.stream().parallel().collect(Collectors.toList()); // Compliant
    list.stream().sequential(); // Noncompliant
    list.stream().sequential().collect(Collectors.toList()); // Compliant
    list.stream().onClose(() -> System.out.println("closed")); // Noncompliant
    list.stream().onClose(() -> System.out.println("closed")).collect(Collectors.toList()); // Compliant
  }

  void constructors() {
    new StreamParamInConstructor(IntStream.range(0, 10).filter(e -> true)); // Compliant
  }

  class StreamParamInConstructor {
    public StreamParamInConstructor(Stream stream) {
    }
  }
}
