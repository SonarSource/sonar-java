
class A {
  public static int counter = 0;
  public int nonStaticCounter = 0;
  public C c = new C();
  public D d = new D();
  public class C {
    public int counter = 0;
    public D d = new D();
  }
  public static class D {
    public static int counter = 0;
    public static class E {
      public static int counter = 0;
    }
  }
  public static int method() {
    return 0;
  }
  public D d() {
    return d;
  }
}

class B {
  private A first = new A();
  private A second = new A();
  private A.D third = new A.D();

  public A.D d() {
    return third;
  }

  public void noncompliant() {
    first.counter ++; // Noncompliant [[sc=5;ec=18]] {{Change this instance-reference to a static reference.}}
    second.counter ++; // Noncompliant
    second.method(); // Noncompliant
    third.counter ++; // Noncompliant
    first.d.counter++; // Noncompliant
    first.c.d.counter++; // Noncompliant
    first.d().counter++; // Noncompliant
    d().counter++; // Noncompliant
    ((A.D) d()).counter++; // Noncompliant [[sc=5;ec=24]]
    (d()).counter++; // Noncompliant
    A.D[] darray = new A.D[1];
    darray[0].counter++; // Noncompliant
  }

  public void compliant() {
    A.counter ++;
    A.D.counter ++;
    A.D.E.counter ++;
    first.nonStaticCounter ++;
    first.c.counter ++;
  }
}
abstract class Test {

  void test(java.util.function.Supplier<?> s) {
    this.call(Object::new);
    this.call(() -> new Object());
    this.call(s);
  }

  static void call(java.util.Set<?> o) {}

  abstract void call(java.util.function.Supplier<?> supplier);
}
