package checks;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class LambdaA {
  void fun() {
    IntStream.range(1, 5)
        .map((x) -> x * x)
        .map(x -> square(x)) // Noncompliant [[sc=16;ec=18]] {{Replace this lambda with a method reference.}}
        .map(x -> { // Noncompliant
          return square(x);
        })
        .map(this::square) //Compliant
        .forEach(System.out::println);
    IntStream.range(1, 5).forEach(x -> System.out.println(x)); // Noncompliant
    IntStream.range(1, 5).forEach(x -> { // Noncompliant
          System.out.println(x);
        });
    IntStream.range(1, 5).forEach(x -> {return;}); // Compliant

    Arrays.asList("bar").stream().filter(string -> string.startsWith("b")); // Compliant
    Arrays.asList(new LambdaA()).stream().filter(a -> a.coolerThan(0, a)); // Compliant

    biConsumer((x, y) -> { ; });
    biConsumer((x, y) -> { ;; });
  }

  void biConsumer(BiConsumer consumer) {
  }

  void runnable(Runnable runnable) {
  }

  void foo(List<String> list, String a) {
    list.stream().map(String::toLowerCase).close();

    list.stream().map(s -> s.toLowerCase()).close(); // Noncompliant
    list.stream().map(s -> { return s.toLowerCase(); }).close(); // Noncompliant

    list.stream().map(s -> new String()).close(); // Compliant
    list.stream().map(s -> a.toLowerCase()).close(); // Compliant
    list.stream().map(s -> s.toLowerCase().toUpperCase()).close(); // Compliant
    list.stream().forEach(s -> fun()); // Compliant
    list.stream().reduce((x, y) -> x.toLowerCase()); // Compliant
  }

  int square(int x) {
    return x * x;
  }
  
  boolean coolerThan(int i, LambdaA a) {
    return true;
  }

  Collection<Number> values = transform(
    input -> getValueProvider().apply(input).getValue() //cannot be replaced by  a method reference.
  );

  Collection<Number> values2 = transform2((input, input2) -> getValueProvider().apply(input)); //cannot be replaced by  a method reference.

  LambdaA getValueProvider() {
    return null;
  }
  LambdaA getValue() {
    return null;
  }
  LambdaA apply(LambdaA a) {
    return a;
  }

  Collection transform(F f) {return null;}
  Collection transform2(F2 f) {return null;}
  interface F2 {
    LambdaA apply(LambdaA a1, LambdaA a2);
  }
  interface F {
    LambdaA apply(LambdaA a1);
  }

  void nullChecks(List<String> strings, String s2) {
    strings.stream().filter(s -> s != null); // Noncompliant {{Replace this lambda with method reference 'Objects::nonNull'.}}
    strings.stream().filter(s -> { return s != null; }); // Noncompliant {{Replace this lambda with method reference 'Objects::nonNull'.}}
    strings.stream().filter(s -> (s) == null); // Noncompliant {{Replace this lambda with method reference 'Objects::isNull'.}}
    strings.stream().filter(s -> null == s); // Noncompliant {{Replace this lambda with method reference 'Objects::isNull'.}}
    strings.stream().filter(s -> (((s == null)))); // Noncompliant {{Replace this lambda with method reference 'Objects::isNull'.}}

    strings.stream().filter(Objects::nonNull); // Compliant
    strings.stream().filter(Objects::isNull); // Compliant

    strings.stream().filter(s -> (((s == s2)))); // Compliant
    strings.stream().filter(s -> (((s2 == s)))); // Compliant
    strings.stream().filter(s -> (((s2 == null)))); // Compliant
    strings.stream().filter(s -> (((null == null)))); // Compliant
  }

  void append(StringBuilder sb, List<Object> list) {
    list.forEach(item -> sb.append("\n").append(item));
  }
}

class LambdaD {

  LambdaD(Object o) { /* ... */ }
  String foo() { return ""; }
  void bar(java.util.function.Supplier<String> supplier) { /* ... */ }

  void test(Object param) {
    bar(() -> new LambdaD(param).foo()); // Compliant - this is not equivalent to the next line
    bar(new LambdaD(param)::foo);
  }
}

class TestA {
  
}


class CastCheck {

  void bar(java.util.function.Function<Object, String> function) { /* ... */ }
  void bar2(java.util.function.BiFunction<Object, Object, String> function) { /* ... */ }
  void bar3(java.util.function.Function<Object, List<String>> function) { /* ... */ }
  void bar4(java.util.function.Function<Object, TestA> function) { /* ... */ }
  void bar5(java.util.function.Function<Object, TestA[]> function) { /* ... */ }
  void bar6(java.util.function.Function<Object, TestA[][]> function) { /* ... */ }
  void bar7(java.util.function.Function<Object, List<TestA[][]>> function) { /* ... */ }
  void bar8(java.util.function.Function<Object, List<TestA[][]>[]> function) { /* ... */ }
  void bar9(java.util.function.Function<Object, Character> f) { /* ... */ }
  
  void  barbar(java.util.function.Predicate<Object> function) { /* ... */ }
  void barbar2(java.util.function.BiPredicate<Object, String> function) { /* ... */ }
  void barbar3(java.util.function.Predicate<List<String>> function) { /* ... */ }
  void barbar4(java.util.function.Predicate<Object> function) { /* ... */ }
  void barbar5(java.util.function.Predicate<Object[]> function) { /* ... */ }
  void barbar6(java.util.function.Predicate<Object[][]> function) { /* ... */ }
  void barbar7(java.util.function.Predicate<List<TestA[][]>> function) { /* ... */ }
  void barbar8(java.util.function.Predicate<List<TestA[][]>[]> function) { /* ... */ }
  void barbar9(java.util.function.Predicate<Object> f) { /* ... */ }

  void testInstanceOf(Object param) {
    barbar((o) -> o instanceof String); // Noncompliant {{Replace this lambda with method reference 'String.class::isInstance'.}}
    barbar(String.class::isInstance); // Compliant
  
    barbar4((o) -> o instanceof TestA); // Noncompliant {{Replace this lambda with method reference 'TestA.class::isInstance'.}}
    barbar4(TestA.class::isInstance); //Compliant
  
    barbar3(List.class::isInstance); //Compliant
    barbar3((o) -> o instanceof List); // Noncompliant {{Replace this lambda with method reference 'List.class::isInstance'.}}

    barbar5(TestA[].class::isInstance); //Compliant
    barbar5((o) -> o instanceof TestA[]); // Noncompliant {{Replace this lambda with method reference 'TestA[].class::isInstance'.}}

    barbar6(TestA[][].class::isInstance); //Compliant
    barbar6((o) -> o instanceof TestA[][]); // Noncompliant {{Replace this lambda with method reference 'TestA[][].class::isInstance'.}}

    barbar7(List.class::isInstance); //Compliant
    barbar7((o) -> o instanceof List ); // Noncompliant {{Replace this lambda with method reference 'List.class::isInstance'.}}

    barbar8(List[].class::isInstance); //Compliant
    barbar8((o) -> o instanceof List[] ); // Noncompliant {{Replace this lambda with method reference 'List[].class::isInstance'.}}

    barbar9(char.class::isInstance); //Compliant
    barbar9(Character.class::isInstance); //Compliant
    barbar9((o) -> o instanceof Character); // Noncompliant {{Replace this lambda with method reference 'Character.class::isInstance'.}}
  }

  void testInstanceOf2(Object param) {
    barbar((o) -> { // Noncompliant {{Replace this lambda with method reference 'String.class::isInstance'.}}
      return o instanceof String;
    });
  }

  void testInstanceOf3(Object param) {
    barbar2((a, b) -> { // Compliant
      return a instanceof String;
    });
  }

  void testInstanceOf4(Object param) {
    barbar(o -> { // Compliant
      return o instanceof String && true;
    });
  }

  void testInstanceOf5(Object param) {
    Object o2 =  new Object();
    barbar(o -> { // Compliant
      return o2 instanceof String;
    });
  }
  
  void testCasts(Object param) {
    bar((o) -> (String)o); // Noncompliant {{Replace this lambda with method reference 'String.class::cast'.}}
    bar(String.class::cast); // Compliant
  
    bar4((o) -> (TestA) o); // Noncompliant {{Replace this lambda with method reference 'TestA.class::cast'.}}
    bar4(TestA.class::cast); //Compliant
  
    bar3(List.class::cast); //Compliant
    bar3((o) -> (List<String>) o); // Noncompliant {{Replace this lambda with method reference 'List.class::cast'.}}

    bar5(TestA[].class::cast); //Compliant
    bar5((o) -> (TestA[]) o); // Noncompliant {{Replace this lambda with method reference 'TestA[].class::cast'.}}

    bar6(TestA[][].class::cast); //Compliant
    bar6((o) -> (TestA[][]) o); // Noncompliant {{Replace this lambda with method reference 'TestA[][].class::cast'.}}

    bar7(List.class::cast); //Compliant
    bar7((o) -> (List<TestA[][]>) o); // Noncompliant {{Replace this lambda with method reference 'List.class::cast'.}}

    bar8(List[].class::cast); //Compliant
    bar8((o) -> (List<TestA[][]>[]) o); // Noncompliant {{Replace this lambda with method reference 'List[].class::cast'.}}

    bar9(char.class::cast); //Compliant
    bar9(Character.class::cast); //Compliant
    bar9((o) -> (char) o); // Noncompliant {{Replace this lambda with method reference 'char.class::cast'.}}
    bar9((o) -> (Character) o); // Noncompliant {{Replace this lambda with method reference 'Character.class::cast'.}}
  }

  void testCasts2(Object param) {
    bar((o) -> { // Noncompliant {{Replace this lambda with method reference 'String.class::cast'.}}
      return (String)o;
    });
  }

  void testCasts3(Object param) {
    bar2((a, b) -> { // Compliant
      return (String)a;
    });
  }

  void testCasts4(Object param) {
    bar(o -> { // Compliant
      return (String)o.getClass().getCanonicalName();
    });
  }

  void testCasts5(Object param) {
    Object o2 =  new Object();
    bar(o -> { // Compliant
      return (String)o2;
    });
  }
}

