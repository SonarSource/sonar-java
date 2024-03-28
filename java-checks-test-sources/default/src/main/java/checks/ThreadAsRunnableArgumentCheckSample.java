package checks;

import java.util.function.Supplier;

class ThreadAsRunnableArgumentCheckSample {

  ThreadAsRunnableArgumentCheckSample (int i) {
  }

  public void foo() {
    Thread t = new Thread() {
    };
    new Thread(t).start(); // Noncompliant [[sc=16;ec=17]] {{Replace this Thread instance with an instance of Runnable.}}

    new Thread(bar()).start(); // Noncompliant [[sc=16;ec=21]]

    MyThread myThread = new MyThread();
    new Thread(myThread).start(); // Noncompliant

    Runnable r = new Runnable() {
      @Override
      public void run() {
      }
    };
    new Thread(r).start(); // Compliant

    new ThreadAsRunnableArgumentCheckSample (0);

    MyClass m = new MyClass(myThread); // Noncompliant
    m.foo(myThread); // Noncompliant
    m = new MyClass(0, new MyThread()); // Noncompliant
    // Noncompliant@+1
    m = new MyClass(0, myThread, r, new MyThread()); // Noncompliant because of arg1 and arg3
    m = new MyClass(0, new Thread[] {myThread, new MyThread()}); // Noncompliant [[sc=24;ec=63]] {{Replace this Thread[] instance with an instance of Runnable[].}}
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

  private void testCases() {
    var l1 = new Thread(() -> {}); // Compliant
    Runnable l2 = new Thread(() -> {}); // Noncompliant
    Thread l3 = new Thread(() -> {}); // Compliant
    Runnable l4;

    l1 = new Thread(() -> {}); // Compliant
    l2 = new Thread(() -> {}); // Noncompliant
    fieldRunnable = new Thread(() -> {}); // Noncompliant
    fieldThread = new Thread(() -> {}); // Compliant

    foo1(); // Compliant
    foo2(new Thread(() -> {})); // Noncompliant
    foo3(new Thread(() -> {})); // Compliant
    foo4(1,2, new String[0]); // Compliant
    foo5(1,2, new Thread(() -> {}), new String[0]); // Compliant
    foo6(1,2, new Thread(() -> {}), new String[0]); // Noncompliant

    var foo = new Foo(1,2, new Thread(() -> {}), new String[0]); // Noncompliant

    Thread[] threads1 = new Thread[]{}; // Compliant
    Thread[] threads2 = new Thread[]{l1}; // Compliant
    Thread[] threads3 = new Thread[]{l1, fieldThread}; // Compliant

    Runnable[] runnables1 = new Thread[]{}; // Noncompliant
    Runnable[] runnables2 = new Thread[]{l1}; // Noncompliant
    Runnable[] runnables3 = new Thread[]{l1, fieldThread}; // Noncompliant

    Runnable[] runnables4 = new Runnable[]{}; // Compliant
    Runnable[] runnables5 = new Runnable[]{l1}; // Noncompliant
    Runnable[] runnables6 = new Runnable[]{l1, fieldRunnable}; // Noncompliant

    Runnable[] runnables7 = new Runnable[]{l2}; // Compliant
    Runnable[] runnables8 = new Runnable[]{l2, fieldRunnable}; // Compliant
    Runnable[] runnables9 = new Runnable[]{l2, fieldThread}; // Noncompliant

    threads2[0] = l1; // Compliant
    runnables7[0] = l1; // Noncompliant
    runnables7[0] = l2; // Compliant

    foo7(threads1); // Compliant
    foo8(threads1); // Noncompliant
    foo8(runnables1); // Compliant
    foo2(threads1[0]); // Noncompliant
  }

  private Runnable fieldRunnable = new Thread(() -> {}); // Noncompliant

  private Thread fieldThread = new Thread(() -> {}); // Compliant

  private Runnable fieldRunnableUninitialized; // Compliant

  private void foo1() {
  }

  private void foo2(Runnable r) {
  }

  private void foo3(Thread t) {
  }

  private void foo4(int a, int b, String[] c) {
  }

  private void foo5(int a, int b, Thread t, String[] c) {
  }

  private void foo6(int a, int b, Runnable r, String[] c) {
  }

  private void foo7(Thread[] threads) {
  }

  private void foo8(Runnable[] runnables) {
  }

  private static class Foo {
    public Foo(int a, int b, Runnable r, String[] c) {
    }
  }
  private int return1() {
    return 42; // Compliant
  }

  private Thread return2() {
    return new Thread(() -> {}); // Compliant
  }

  private Runnable return3() {
    var a = new Thread(() -> {}); // Compliant
    return a; // Noncompliant
  }

  private Runnable return4() {
    Runnable a = new Thread(() -> {}); // Noncompliant
    return a; // Compliant
  }

  private void return5() {
    return; // Compliant
  }

  private void lambda1(Runnable r) {

    Supplier<Thread> s1 = () -> {
      return new Thread(() -> {}); // Compliant
    };

    Supplier<Runnable> s2 = () -> {
      return new Thread(() -> {}); // Noncompliant
    };

    Supplier<Runnable> s3 = () -> {
      return r; // Compliant
    };
  }

  private void yield1(int code, Thread t, Runnable r) {
    Runnable result1 = switch (code) {
      case 0 -> t; // Noncompliant
      case 1 -> t; // Noncompliant
      default -> null;
    };

    Runnable result2 = switch (code) { // Compliant, switch type is Runnable
      case 0 -> r;
      case 1 -> r;
      default -> null;
    };

    var result3 = switch (code) { // Compliant, switch type is something else
      case 0 -> 42;
      case 1 -> 23;
      default -> 5;
    };

    Runnable result4 = switch (code) {
      case 0 -> r;
      case 1 -> t; // Noncompliant, common supertype is Runnable
      default -> null;
    };

    Runnable result5 = switch (code) {
      case 0 -> r;
      case 1 -> {
        System.out.println();
        yield t; // Noncompliant, common supertype is Runnable
      }
      default -> null;
    };

    Runnable result6 = switch (code) {
      case 0 -> r; // Compliant
      case 1 -> {
        System.out.println();
        yield r; // Compliant
      }
      default -> null;
    };
  }

  private void arraySubtypeAssign(MyThread[] mts) {
    Runnable[] rs = mts; // Noncompliant
  }

  private void arrayInstantiationWithNulltype() {
    String[] words = { "abc" };
  }

  private void switchWithYieldNulltype(int code) {
    switch(code) {
      default -> voidFun();
    };
  }

  private void voidFun() {}
}
