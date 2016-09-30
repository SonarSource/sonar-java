import java.util.List;
import java.util.Optional;

class A {
  void foo(B b) {
    Optional.of(b)
      .flatMap(A::flatipus1)
      .flatMap(A::flatipus2);
  }

  private static Optional<C> flatipus1(B b) {
    return Optional.empty();
  }

  private static Optional<B> flatipus2(C c) {
    return Optional.empty();
  }

  void bar(List<C> cs) {
    cs.stream()
      .map(B.class::cast)
      .filter(A::bool);
  }

  private static boolean bool(B b) {
    return true;
  }
}

class B {}
class C extends B {}
