import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract class A {
  public void foo() {
    foo((String s) -> s.length()); // Compliant
    foo(s -> s.length()); // compliant : no block and one param
    foo((String a, Object b) -> a.length()); // Compliant
    foo((a, b) -> a.length()); // compliant two params and no block
    foo((a, b) -> {return a.length();}); // Noncompliant [[sc=10;ec=14]] {{Specify a type for: 'a', 'b'}}
    foo(s -> {return s.length();}); // Noncompliant [[sc=9;ec=10]] {{Specify a type for: 's'}}
    foo((a, b, c) -> a.length()); // Noncompliant [[sc=10;ec=17]] {{Specify a type for: 'a', 'b', 'c'}}
    foo((a, b, c) -> {return a.length();}); // Noncompliant [[sc=10;ec=17]] {{Specify a type for: 'a', 'b', 'c'}}
  }

  abstract void foo(Consumer<String> s);
  abstract void foo(BiConsumer<String, Object> bc);
  abstract void foo(TriConsumer tc);

  private interface TriConsumer {
    String fun(String a, String b, String c);
  }
}
