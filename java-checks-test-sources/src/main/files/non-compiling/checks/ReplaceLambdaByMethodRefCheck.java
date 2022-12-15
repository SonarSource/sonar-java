package checks;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class A {
  void fun() {
    Function<String, String> debug = x -> unknown(x);  // Compliant, do not propose to replace with "!unknown!::!unknownMethod!"

    Arrays.asList(new A()).stream().filter(a -> a.coolerThan(0, a)); // Compliant

  }

  void biConsumer(BiConsumer consumer) {
  }

  void runnable(Runnable runnable) {
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
      biConsumer((xParam, y) -> unknown(xParam , y)); // Compliant, do not propose to replace with "!unknown!::!unknownMethod!"
      biConsumer((x, y) -> unknown(y , x));
      biConsumer((x, y) -> unknown(y));
      biConsumer((x,y) -> new ClassTree(x, y)); // Noncompliant
      biConsumer((x,y) -> new ClassTree(y, x));
      biConsumer((x,y) -> new ClassTree(x, y) {
        //can get some capture
      });
      runnable(() -> unknown()); // Compliant, shorter
    }
  }

  void nullChecks(List<String> strings, String s2) {
    strings.stream().filter(s -> (s) == null); // Noncompliant {{Replace this lambda with method reference 'Objects::isNull'.}}
  }

  void append(StringBuilder sb, List<Object> list) {
    list.forEach(item -> sb.append("\n").foo.append(item));
    list.forEach(item -> this.append(item)); // Compliant, do not propose to replace with "!unknown!::!unknownMethod!"
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

class AmbiguousMethods {

  public static void main(String[] args) {
    Function<Ambiguous, String> f = a -> a.f();  // Compliant, A::f is ambiguous

    Function<NotAmbiguous1, String> f2 = ambig -> ambig.f();  // Noncompliant
    Function<NotAmbiguous2, String> f3 = a -> a.f(a);  // FN, could be replaced by NotAmbiguous2::f

    Function<Ambiguous, String> f4 = a -> a.unknown();  // Compliant, A::f is ambiguous
    Function<Unknown, String> f4 = a -> a.unknown();  // Compliant, A::f is ambiguous
    Function<AmbiguousChild, String> f5 = a -> a.f();  // Compliant, A::f is ambiguous

    Function<String, String> f6 = x -> unknown(x);  // Compliant, do not propose to replace with "!unknown!::!unknownMethod!"
    Function<String, String> f6 = x -> unknown1.unknown2(x);  // Compliant
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

  void test(Object param) {

    bar4((object) -> (TestXXXXXX) object); // Noncompliant {{Replace this lambda with method reference 'TestXXXXXX.class::cast'.}}
    bar4(TestXXXXXX.class::cast); //Compliant

    bar4((o) -> (TestXXXXXX<ABC>) o); // Compliant, there is no 'TestXXXXXX<ABC>.class::cast'

    bar5((object) -> (TestA[]) object); // Noncompliant {{Replace this lambda with method reference 'TestA[].class::cast'.}}
  }

}




