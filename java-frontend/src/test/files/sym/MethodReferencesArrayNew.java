import java.util.List;

class A {
  void foo(List<B> b, List<Integer> sizes) {
    bar(b.stream().toArray(B[][]::new));

    sizes.stream()
      .map(B[]::new)
      .filter(A::bool);
  }

  void bar(B[][] b) {}
  static boolean bool(B[] b) { return true; }
}

class B {}
