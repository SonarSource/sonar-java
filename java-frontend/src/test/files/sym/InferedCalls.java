import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class A {
  public static class MyClass { }

  public void test(List<MyClass> list) {

    Optional<MyClass> c1 = list.stream()
      .collect(
      Collectors.reducing(
        (t1, t2) ->
          this.combine1(t1, t2)));
    Optional<MyClass> c2 = list.stream().collect(
      Collectors.reducing(
      this::combine2));
  }

  private MyClass combine1(MyClass t1, MyClass t2) {
    return t1;
  }

  private MyClass combine2(MyClass t1, MyClass t2) {
    return t1;
  }
}
