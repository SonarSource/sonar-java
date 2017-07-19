class A extends Thread{ // Noncompliant [[sc=7;ec=8]] {{Don't extend "Thread", since the "run" method is not overridden.}}
  int run;
  void run(Object stuff) {
    // not the run() method
  }
}

class B extends A { //Compliant, does not directly extend Thread
}

class C extends Thread { //Compliant
  void run() { }
}
class D {
  void foo(Runnable r){
    Thread t1 = new Thread() { };// Noncompliant [[sc=21;ec=27]] {{Don't extend "Thread", since the "run" method is not overridden.}}
    Thread t2 = new Thread() {//Compliant
      @Override
      public void run() {
        super.run();
      }
    };
    Thread t3 = new Thread(r) { // Compliant
      void doSomething() { /* do nothing */ }
    };
  }
}
class E {

}

class F extends Thread { // Compliant - has a constructor which call 'super(...)' with a runnable as argument
  F(Runnable r) {
    super(r);
  }
}

class G extends Thread { // Noncompliant {{Don't extend "Thread", since the "run" method is not overridden.}}
  G(Runnable r) {
    super("hello world");
    foo();
  }
}

class H extends Thread { // Noncompliant {{Don't extend "Thread", since the "run" method is not overridden.}}
  H(Runnable r) {
    class I extends Thread { // inner class calling super - Compliant
      I(Runnable r) {
        super(r);
      }
    }
  }
}

enum MyEnum {
  FOO {
    void fun(){}
  }, BAR;
  void fun() {}
}