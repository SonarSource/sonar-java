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
  }

  int bar(A a1, A a2) {
    return 1;
  }
  static int qix(A a1, A a2) {
    return 1;
  }


  void methodReference(Comparator<A> comp) {
  }


}