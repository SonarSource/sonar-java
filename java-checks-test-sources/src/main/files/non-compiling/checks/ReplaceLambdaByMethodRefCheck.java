package checks;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class A {
  void fun() {
    IntStream.range(1, 5)
        .map((x) -> x * x)
        .map(x -> square(x)) // Noncompliant [[sc=16;ec=18]] {{Replace this lambda with method reference 'this::square'.}}
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
    Arrays.asList(new A()).stream().filter(a -> a.coolerThan(0, a)); // Compliant

    biConsumer((x, y) -> x * y);
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

  boolean coolerThan(int i, A a) {
    return true;
  }

  Collection<Number> values = transform(
    input -> getValueProvider().apply(input).getValue() //cannot be replaced by  a method reference.
  );

  Collection<Number> values2 = transform2((input, input2) -> getValueProvider().apply(input)); //cannot be replaced by  a method reference.

  A getValueProvider() {
    return null;
  }
  A getValue() {
    return null;
  }
  A apply(A a) {

  }

  Collection transform(F f) {return null;}
  Collection transform2(F2 f) {return null;}
  interface F2 {
    A apply(A a1, A a2);
  }
  interface F {
    A apply(A a1);
  }

  public interface Query extends Unwrappable {
    Collection<String> keys();
    default String get(String name) {
    }

    default Map<String, String> keyValues() {
      return keys().stream().collect(Collectors.toMap(key -> key, key -> get(key))); // Noncompliant
    }

    default void process(String s1, String s2, int i){}
    default void fun2(){
      IntStream.range(1, 5).forEach(i -> { process("foo", "bar", i); });
      biConsumer((xParam, y) -> myMethod(xParam , y)); // Noncompliant
      biConsumer((x, y) -> myMethod(y , x));
      biConsumer((x, y) -> myMethod(y));
      biConsumer((x,y) -> new ClassTree(x, y)); // Noncompliant
      biConsumer((x,y) -> new ClassTree(y, x));
      biConsumer((x,y) -> new ClassTree(x, y) {
        //can get some capture
      });
      runnable(() -> myMethod()); // Compliant, shorter
    }
  }

  void nullChecks(List<String> strings, String s2) {
    strings.stream().filter(string -> string != null); // Noncompliant {{Replace this lambda with method reference 'Objects::nonNull'.}}
    strings.stream().filter(s -> { return s != null; }); // Noncompliant {{Replace this lambda with method reference 'Objects::nonNull'.}}
    strings.stream().filter(s -> (s) == null); // Noncompliant {{Replace this lambda with method reference 'Objects::isNull'.}}
    strings.stream().filter(string -> null == string); // Noncompliant {{Replace this lambda with method reference 'Objects::isNull'.}}
    strings.stream().filter(s -> (((s == null)))); // Noncompliant {{Replace this lambda with method reference 'Objects::isNull'.}}

    strings.stream().filter(Objects::nonNull); // Compliant
    strings.stream().filter(Objects::isNull); // Compliant

    strings.stream().filter(s -> s != null); // Compliant, shorter
    strings.stream().filter(s -> s == null); // Compliant, shorter

    strings.stream().filter(s -> (((s == s2)))); // Compliant
    strings.stream().filter(s -> (((s2 == s)))); // Compliant
    strings.stream().filter(s -> (((s2 == null)))); // Compliant
    strings.stream().filter(s -> (((null == null)))); // Compliant
  }

  void append(StringBuilder sb, List<Object> list) {
    list.forEach(item -> sb.append("\n").append(item));
    list.forEach(item -> sb.append("\n").foo.append(item));
    list.forEach(item -> this.append(item)); // Noncompliant
  }
}

class B {
  String str;
  String toUpperCase() { return str.toUpperCase(); }
}

class C extends B {
  B b1;
  static B b2;
  final B b3 = new B();

  public void foo() {
    java.util.function.Supplier<String> s0 = () -> b1.toUpperCase(); // Compliant
    java.util.function.Supplier<String> s1 = () -> b2.toUpperCase(); // Compliant
    java.util.function.Supplier<String> s2 = () -> b3.toUpperCase(); // Noncompliant
    java.util.function.Supplier<String> s3 = () -> this.toUpperCase(); // Noncompliant
    java.util.function.Supplier<String> s4 = () -> super.toUpperCase(); // Noncompliant
  }

  void bar() {
    b1 = new C();
    b1.str = "hello";

    java.util.function.Supplier<String> s = () -> (this.b1).toUpperCase(); // Compliant

    System.out.println(s.get()); // HELLO
    b1 = new C();
    b1.str = "world";
    System.out.println(s.get()); // WORLD
  }
}

public class D {

  D(Object o) { /* ... */ }
  String foo() { return ""; }
  void bar(java.util.function.Supplier<String> supplier) { /* ... */ }

  void test(Object param) {
    bar(() -> new D(param).foo()); // Compliant - this is not equivalent to the next line
    bar(new D(param)::foo);
  }
}

class AmbiguousMethods {

  public static void main(String[] args) {
    Function<Ambiguous, String> f = a -> a.f();  // Compliant, A::f is ambiguous

    Function<NotAmbiguous1, String> f2 = ambig -> ambig.f();  // Noncompliant
    Function<NotAmbiguous2, String> f3 = a -> a.f(a);  // FN, could be replaced by NotAmbiguous2::f

    Function<Ambiguous, String> f4 = a -> a.unknown();  // Compliant, A::f is ambiguous
    Function<Unknown, String> f4 = a -> a.unknown();  // Compliant, A::f is ambiguous
    Function<AmbiguousChild, String> f5 = a -> a.f();  // Compliant, A::f is ambiguous
  }
}

class Ambiguous {

  String f() {
    return "";
  }

  static String f(Ambiguous a) {
    return "";
  }

}

class AmbiguousChild extends Ambiguous {
}

class NotAmbiguous1 {
  String f() {
    return "";
  }
}

class NotAmbiguous2 {
  static String f(NotAmbiguous2 a) {
    return "";
  }
}


class CastCheck {

  void bar(java.util.function.Function<Object, String> function) { /* ... */ }
  void bar2(java.util.function.BiFunction<Object, Object, String> function) { /* ... */ }
  void bar3(java.util.function.Function<Object, List<String>> function) { /* ... */ }
  void bar4(java.util.function.Function<Object, TestA> function) { /* ... */ }
  void bar5(java.util.function.Function<Object, TestA[]> function) { /* ... */ }

  void test(Object param) {
    bar((o) -> (String)o); // Compliant, shorter
    bar((object) -> (String)object); // Noncompliant {{Replace this lambda with method reference 'String.class::cast'.}}
    bar(String.class::cast); // Compliant

    bar4((object) -> (TestXXXXXX) object); // Noncompliant {{Replace this lambda with method reference 'TestXXXXXX.class::cast'.}}
    bar4(TestXXXXXX.class::cast); //Compliant

    bar4((o) -> (TestXXXXXX<ABC>) o); // Compliant, there is no 'TestXXXXXX<ABC>.class::cast'

    bar3(List.class::cast); //Compliant
    bar3((o) -> (List<String>) o); // Compliant, there is no 'List<String>.class::cast'

    bar5(TestA[].class::cast); //Compliant
    bar5((object) -> (TestA[]) object); // Noncompliant {{Replace this lambda with method reference 'TestA[].class::cast'.}}
  }

  void test2(Object param) {
    bar((o) -> { // Noncompliant {{Replace this lambda with method reference 'String.class::cast'.}}
      return (String)o;
    });
  }

  void test3(Object param) {
    bar2((a, b) -> { // Compliant
      return (String)a;
    });
  }

  void test4(Object param) {
    bar(o -> { // Compliant
      return (String)o.getClass().getCanonicalName();
    });
  }

  void test5(Object param) {
    Object o2 =  new Object();
    bar(o -> { // Compliant
      return (String)o2;
    });
  }
}




