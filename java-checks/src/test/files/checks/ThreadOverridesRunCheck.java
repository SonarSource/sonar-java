class A extends Thread{ //NonCompliant

}

class B extends A { //Compliant, does not directly extend Thread

}

class C extends Thread { //Compliant
  void run() {

  }
}
class D {
  void run(){
    Thread t1 = new Thread() {//NonCompliant

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