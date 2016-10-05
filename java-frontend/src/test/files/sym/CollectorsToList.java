import java.util.stream.Stream;
import java.util.stream.Collectors;

class ReturnTypeInference {
  private void foo(java.util.List<String> l) {}
  private void foo2(java.util.Collection<String> l) {}
  void test() {
    java.util.List<String> l;
    foo(l.stream().sorted().collect(Collectors.toList()));
    foo2(l.stream().collect(Collectors.toCollection(() -> new java.util.LinkedHashSet<String>())));
  }
}