abstract class A {
  void foo(java.util.Collection<A> objects) {
    objects.stream().map(A::<String>getValue);
  }

  abstract <T> T getValue();
}
