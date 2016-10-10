import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class A {
  void qix(Set<String> set) {
    set.stream()
      .map(key -> key.startsWith("hello") ? key.length() : key)
      .collect(Collectors.toList())
      .forEach(A::foo);
  }

  private static void foo(Object o) { }
}
