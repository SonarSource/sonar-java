package checks;
class ThreadAsRunnableArgumentCheck {

  ThreadAsRunnableArgumentCheck (int i) {
  }

  public void foo() {
    Thread t = new Thread() {
    };
    new Thread(t).start(); // Noncompliant [[sc=16;ec=17]] {{Replace Thread "t" with an instance of Runnable.}}

    new Thread(bar()).start(); // Noncompliant [[sc=16;ec=21]]

    MyThread myThread = new MyThread();
    new Thread(myThread).start(); // Noncompliant

    Runnable r = new Runnable() {
      @Override
      public void run() {
      }
    };
    new Thread(r).start(); // Compliant

    new ThreadAsRunnableArgumentCheck (0);

    MyClass m = new MyClass(myThread); // Noncompliant
    m.foo(myThread); // Noncompliant
    m = new MyClass(0, new MyThread()); // Noncompliant
    // Noncompliant@+1
    m = new MyClass(0, myThread, r, new MyThread()); // Noncompliant because of arg1 and arg3
    m = new MyClass(0, new Thread[] {myThread, new MyThread()}); // Noncompliant {{Replace Thread "Argument 2" with an instance of Runnable[].}}
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
  class MyThread extends Thread {
  }

  class MyClass {
    MyClass(Runnable r) {
    }

    MyClass(int i, String s) {
    }

    MyClass(int i, Runnable... runners) {
    }

    void bar(Thread thread) {

    }

    void foo(Runnable r) {
    }

    void qix(Runnable... runners) {
    }
  }
}


