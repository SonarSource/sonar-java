class MyClass<TYPE> {
  <TYPE> void addAll(Collection<TYPE> c) {
  }
}

public class MyClass2<T> {
  <T> void addAll(Collection<T> c) {
  }
  <T1 extends T, T2 extends T> void addAll(Collection<T1> c1, Collection<T2> c2) {
  }
}
