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

    MyClass m = new MyClass(myThread); // Noncompliant
    m.foo(myThread); // Noncompliant
    m = new MyClass(0, new MyThread()); // Noncompliant
    m = new MyClass(0, myThread, r, new MyThread()); // Noncompliant because of arg1 and arg3
    m = new MyClass(0, new Thread[] {myThread, new MyThread()}); // Noncompliant
    m = new MyClass(0); // Compliant
    m = new MyClass(0, new Runnable[] {}); // Compliant
    m = new MyClass(0, null, r, null); // Compliant
    m.bar(myThread); // Compliant
    m.qix(); // Compliant
  }

  public Thread bar() {
    return new Thread() {
    };
  }
}

class MyThread extends Thread {
}

class MyClass {
  MyClass(Runnable r) {
  }

  MyClass(int i, String s) {
  }

  MyClass(int i, Runnable... runners) {
  }

  void foo(Runnable r) {
  }

  void qix(Runnable... runners) {
  }
}
