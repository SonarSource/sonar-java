import java.util.concurrent.locks.Condition;

class A {
  void foo() {
    new C().wait();
    new C().wait(1);
    new C().wait(1, 3);
    new B().wait();  // Noncompliant [[sc=13;ec=17]] {{The "Condition.await(...)" method should be used instead of "Object.wait(...)"}}
    Runnable r = new B()::wait; // Noncompliant [[sc=27;ec=31]] {{The "Condition.await(...)" method should be used instead of "Object.wait(...)"}}
    new B().wait(1);  // Noncompliant
    new B().wait(1, 3);  // Noncompliant
  }

  class B implements Condition {

  }
  class C {
  }
}
