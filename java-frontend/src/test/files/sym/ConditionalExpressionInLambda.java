import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class A {
  void qix(Set<String> set) {
    set.stream()
      .map(key -> key.startsWith("hello") ? key.length() : key)
      .collect(Collectors.toList())
      .forEach(A::foo);

    set.stream()
      .flatMap(key -> key.startsWith("hello") ? Stream.empty() : Stream.of(Integer.parseInt(key)))
      .collect(Collectors.toList())
      .forEach(A::bar);
  }

  private static void foo(Object o) { }
  private static void bar(Integer i) { }
}
