import java.lang.Override;
import java.lang.Thread;

class Outer {

  void foo() {
    new A().wait(); //Compliant
    new A().wait(1l); //Compliant
    new A().wait(1,1); //Compliant
    new A().notify(); //Compliant
    new A().notifyAll(); //Compliant

    new B().wait(); //NonCompliant
    new B().wait(1000); //NonCompliant
    new B().wait(12,12); //NonCompliant
    new B().notify(); //NonCompliant
    new B().notifyAll(); //NonCompliant
  }

  class A {

  }

  class B extends Thread {
    @Override
    public void run() {

    }
  }


}