class MyClass<TYPE> {
  <TYPE> void addAll(Collection<TYPE> c) {
  }
}

public class MyClass2<T> {
  <T> void addAll(Collection<T> c) {
  }
  <T0 extends T, T1 extends T> void addAll(Collection<T1> c1, Collection<T0> c2) {
  }
}
