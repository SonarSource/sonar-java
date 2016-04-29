import java.util.Comparator;
import java.util.Stream;

class A {
  A() {
  }
  A(Comparator<A> comp) {
  }

  void foo(){
    methodReference(this::bar);
    methodReference(new A()::bar);
    methodReference(A::qix);
    new A(this::bar);
    methodRefConstructor(A::new);
  }

  int bar(A a1, A a2) {
    return 1;
  }
  static int qix(A a1, A a2) {
    return 1;
  }


  void methodReference(Function<A> comp) {
  }

  void methodRefConstructor(AProducer producer){}

  interface Function<T> {
    int func(A a1, A a2);
  }
  interface AProducer {
    A func();
  }


}