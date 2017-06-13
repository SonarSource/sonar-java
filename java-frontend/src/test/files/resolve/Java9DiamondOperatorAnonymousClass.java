import java.util.ArrayList;
import java.util.List;

public class A {

  List<Integer> foo1() {
    return new ArrayList<>();
  }

  B<Integer> foo2() {
    return new B<Integer>() { };
  }

  B<Integer> foo3(Integer content) {
    return new B<>(content) { };
  }

  <T> B<T> bar(T content) {
    return new B<>(content) { };
  }

  C<Integer, String> qix(Integer content) {
    return new C<>(content, "wello", "horld") { };
  }

  D<Integer> gul() {
    return new D<>() {
      @Override
      public void foo(Integer x) { }
    };
  }

  void blo(B<Integer> d) {
    blo(new B<>(){ });
  }
}

class B<U> {
  public B() { }
  public B(U content) { }
}

class C<U, V> {
  public C(U content, V ... stuffs) { }
}

interface D<X> {
  void foo(X x);
}
