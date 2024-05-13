package checks;

import java.io.Serializable;

class ThreadRunCheckSample {

  {
    Thread t = new Thread(new B());
    t.run(); // Noncompliant
  }

  public static void main(String[] args) {
    Runnable runnable = null;

    Thread myThread = new Thread(runnable);
    myThread.run(); // Noncompliant {{Call the method Thread.start() to execute the content of the run() method in a dedicated thread.}}
//           ^^^

    Thread myThread2 = new Thread(runnable);
    myThread2.start();

    run();
    UselessIncrementCheck a = new UselessIncrementCheck();
    a.run();

    B b = new B();
    b.run(); // Noncompliant

    C c = new C();
    c.run(); // Compliant

    D d = new D();
    d.run(); // Noncompliant

    E e = new E();
    e.run();

    F f = new F();
    f.run();

    runnable.run(); // Compliant
  }

  public static void run() {
  }

  static class B extends Thread {
  }

  static class C implements Runnable {

    @Override
    public void run() {
    }

  }

  static class D extends B {
    @Override
    public void run() {
      C c = new C();
      c.run(); // compliant, within run method
      super.run(); // compliant, within run method
    }
  }

  static class E implements Serializable {

    public void run() {
    }

  }

  static class F extends E {
  }

  void quickFix() {
    Runnable runnable = null;
    Thread myThread = new Thread(runnable);
    myThread.run(); // Noncompliant [[quickfixes=qf1]]
//           ^^^
    // fix@qf1 {{Replace run() with start()}}
    // edit@qf1 [[sc=14;ec=17]] {{start}}
  }
}

