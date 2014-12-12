class A {
  java.lang.String obj;
  void qix() {
    synchronized (obj) {
      wait();
      wait(1, 2);
      wait(1);
      notify();
      notifyAll();
    }
  }
  synchronized void foo() {
    wait();
    wait(1, 2);
    wait(1);
    notify();
    notifyAll();
  }
  void bar() {
    obj.wait();    //NonCompliant
    wait(1, 2); //NonCompliant
    wait(1);   //NonCompliant
    notify();  //NonCompliant
    notifyAll(); //NonCompliant
  }

  synchronized void foo2() {
    wait();
    class A {
      void foo() {
        wait(); //NonCompliant
      }
    }
    wait();
  }


}