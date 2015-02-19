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

    m = new MyClass(myThread, 0); // Compliant, as constructor method symbol can not be resolved
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

  MyClass(int i, Runnable r) {
  }

  void foo(Runnable r) {
  }
}
