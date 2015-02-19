class A {

  A(int i) {
  }

  public void foo() {
    Thread t = new Thread() {
    };
    new Thread(t).start(); // Noncompliant

    new Thread(bar()).start(); // Noncompliant

    MyThread myThread = new MyThread();
    new Thread(myThread).start(); // Noncompliant

    Runnable r = new Runnable() {
      @Override
      public void run() {
      }
    };
    new Thread(r).start(); // Compliant

    new A(0);
  }

  public Thread bar() {
    return new Thread() {
    };
  }

  class MyThread extends Thread {
  }
}
