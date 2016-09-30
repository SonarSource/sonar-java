import java.util.stream.Stream;
import java.util.stream.Collectors;

class ReturnTypeInference {
  private void foo(java.util.List<String> l) {}
  void test() {
    java.util.List<String> l;
    foo(l.stream().sorted().collect(Collectors.toList()));
  }
}