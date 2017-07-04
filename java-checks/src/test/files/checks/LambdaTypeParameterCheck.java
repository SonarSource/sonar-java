import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract class A {
  public void foo() {
    foo((String s) -> s.length()); // Compliant
    foo(s -> s.length()); // Noncompliant [[sc=9;ec=10]] {{Specify a type for: 's'}}
    foo((String a, Object b) -> a.length()); // Compliant
    foo((a, b) -> a.length()); // Noncompliant [[sc=10;ec=14]] {{Specify a type for: 'a', 'b'}}
  }

  abstract void foo(Consumer<String> s);
  abstract void foo(BiConsumer<String, Object> bc);
}
