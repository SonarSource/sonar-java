import java.lang.reflect.Method;

class A {
  void foo() {
    Method m;
    Class<T> c;
    if (m.isAnnotationPresent(Override.class)) { //NonCompliant
    }
    if (c.isAnnotationPresent(Override.class)) { //NonCompliant
    }
    if (m.isAnnotationPresent(Deprecated.class)) { //Compliant, runtime retention
    }
    if (m.isAnnotationPresent(bar())) { //Compliant, we can't know what the bar method is returning.
    }
  }
  Class<? extends Annotation> bar() {
    return null;
  }
}