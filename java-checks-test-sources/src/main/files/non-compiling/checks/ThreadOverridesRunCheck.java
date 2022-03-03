package checks;

class ThreadOverridesRunCheck {
  void foo(Runnable r) {
    Thread t1 = new Thread(r) { // Compliant
      void doSomething() { /* do nothing */ }
    };

    Thread t2 = new Thread(unknown) {
      void doSomething() { /* do nothing */ }
    };

    Thread t3 = new Thread() { }; // Noncompliant
  }

}
