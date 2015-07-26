class A extends Thread{ // Noncompliant {{Stop extending the Thread class as the "run" method is not overridden}}

}

class B extends A { //Compliant, does not directly extend Thread

}

class C extends Thread { //Compliant
  void run() {

  }
}
class D {
  void run(){
    Thread t1 = new Thread() {// Noncompliant {{Stop extending the Thread class as the "run" method is not overridden}}

    };
    Thread t1 = new Thread() {//Compliant
      @Override
      public void run() {
        super.run();
      }
    };
  }
}
class E {

}
enum MyEnum {
  FOO {
    void fun(){}
  }, BAR;
  void fun() {}
}