import java.util.concurrent.locks.Condition;

class A {
  void foo() {
    new C().wait(); //Compliant
    new C().wait(1);//Compliant
    new C().wait(1, 3);//Compliant
    new B().wait();  //NonCompliant
    new B().wait(1);  //NonCompliant
    new B().wait(1, 3);  //NonCompliant
  }

  class B implements Condition {

  }
  class C {
  }
}
