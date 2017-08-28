import java.util.Objects;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

class A {
  Object a,b,c,d;
  Object[] arr;

  void foo() {
    a = b;
    c = a;
    b = c; // Noncompliant [[sc=5;ec=10]] {{Remove this useless assignment; "b" already holds the assigned value along all execution paths.}}
  }

  void bar(boolean test) {
    a = b;
    c = test ? a : d;
    b = c; // Compliant : can be 'b' or 'd'
  }

  void gul(boolean test) {
    a = b;
    c = a;
    b = test ? a : c; // Noncompliant [[sc=5;ec=21]] {{Remove this useless assignment; "b" already holds the assigned value along all execution paths.}}
  }

  void qix() {
    arr[0] = arr[1];
    arr[2] = arr[0];
    arr[1] = arr[2]; // Compliant - array accesses not handled
  }

  Object o1, o2;
  void kil(Object p1, Object p2) {
    Object v1 = (this.o2 == p2) ? this.o1 : p1;
    this.o1 = v1; // Compliant
  }

  void moc(Object param) {
    param = Objects.requireNonNull(param, "should not be null"); // Noncompliant - param is indeed reassigned with its own value
  }

  void puk(@Nullable Object o, List<Object> myList) {
    Stream<Object> stream = myList.stream();
    if (o != null) {
      stream = stream.skip(42L); // Compliant
    }
  }
}

class B {

  static void gul(List<String> items) {
    int index = 1;
    index = foo(items, index); // Compliant
    index = bar(index); // Compliant
    return;
  }

  private static int foo(List<String> items, int index) {
    int newIndex = index;
    for (String item : items) {
      newIndex++;
    }
    return newIndex;
  }

  private static int bar(int index) {
    int newIndex = index;
    newIndex++;
    return newIndex;
  }
}
