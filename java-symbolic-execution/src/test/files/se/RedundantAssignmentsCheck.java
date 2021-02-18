import java.util.Objects;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

class A {
  Object a,b,c,d;

  void foo() {
    a = b; // flow@foo {{Implies 'a' has the same value as 'b'.}}
    c = a; // flow@foo {{Implies 'c' has the same value as 'a'.}}
    b = c; // Noncompliant [[sc=5;ec=10;flows=foo]] {{Remove this useless assignment; "b" already holds the assigned value along all execution paths.}}
  }

  void bar(boolean test) {
    a = b;
    c = test ? a : d;
    b = c; // Compliant : can be 'b' or 'd'
  }

  void xmi(boolean test) {
    a = b; // flow@xmi1 {{Implies 'a' has the same value as 'b'.}} flow@xmi2 {{Implies 'a' has the same value as 'b'.}}
    c = a; // flow@xmi2 {{Implies 'c' has the same value as 'a'.}}
    if (test) {
      d = a; // flow@xmi1 {{Implies 'd' has the same value as 'a'.}}
    } else {
      d = c; // flow@xmi2 {{Implies 'd' has the same value as 'c'.}}
    }
    b = d; // Noncompliant [[sc=5;ec=10;flows=xmi1,xmi2]] {{Remove this useless assignment; "b" already holds the assigned value along all execution paths.}}
  }

  void gul(boolean test) {
    a = b;
    c = a;
    b = test ? a : c; // Noncompliant [[sc=5;ec=21]] {{Remove this useless assignment; "b" already holds the assigned value along all execution paths.}}
  }

  void moc(Object param) {
    param = Objects.requireNonNull(param, "should not be null"); // Noncompliant [[sc=5;ec=64]] - param is reassigned with its own value
  }

  Object p;
  void mad() {
    p = Objects.requireNonNull(p); // Noncompliant - p is reassigned with its own value
  }

  Object[] arr;
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

class C {
  abstract class A {

    void foo() {
      Throwable caughtEx = null;
      caughtEx = bar(caughtEx); // Compliant
    }

    private Throwable bar(Throwable ex) {
      Throwable result = null;
      try {
        doSomething();
      } catch (Exception e) {
        result = e;
      }
      if (result != null) {
        return result; // return another exception
      }
      return ex;
    }

    abstract void doSomething();
  }
}
