import java.lang.Override;
import java.lang.String;

enum foo  {
  FOO{
    @Override
    public String method() {
      return "foo";
    }
  },
  BAR{
    @Override
    public String method() {
      return "bar";
    }
  };


  public String method(){
    return "";
  }
}

interface Handler {
  String handle();
}

interface B {
  default void foo() {}
  default void bar() {}
}

interface C{
  default void foo() {}
  default int bar(int a) {}
}

interface D{
  default void foo2() {}
}

class A {
  void toto() {
    new Handler(){ // Noncompliant {{Make this anonymous inner class a lambda (sonar.java.source not set. Assuming 8 or greater.)}}
      @Override
      public String handle() {
        return "handled";
      }
    }.handle();

    new Handler(){

      private String myMethod(){
        return "plop";
      }

      @Override
      public String handle() {
        return myMethod();
      }
    }.handle();

    new Handler(){
      @Override
      public String handle() {
        return this.toString();
      }
    }.handle();

    new Handler(){ // Noncompliant {{Make this anonymous inner class a lambda (sonar.java.source not set. Assuming 8 or greater.)}}
      @Override
      public String handle() {
        class C{
          String meth(){
            return "";
          }
          String fun(){
            return this.meth();
          }
        }
        return new C().fun();
      }
    };
    
    new Handler(){ // Compliant
      int myVar;
      
      @Override
      public String handle() {
        return "";
      }
    };
    
    new Handler(){ // Noncompliant {{Make this anonymous inner class a lambda (sonar.java.source not set. Assuming 8 or greater.)}}
      @Override
      public String handle() {
        return "";
      }; // this empty statement should not be counted!
    };

    new
    Handler // Noncompliant {{Make this anonymous inner class a lambda (sonar.java.source not set. Assuming 8 or greater.)}}
    (){
      @Override
      public String handle() {
        return A.this.toStr();
      }
    };
   
    new B() {// Compliant
      @Override
      public void bar() {
      }
    };
  
    new C() { // Noncompliant
      @Override
      public void foo() {
      }
    };
   
    new D() {};  // Compliant
  
    new D() { // Noncompliant
      @Override
      public void foo2() {}
    };
  }

  String toStr(){
    return "";
  }

  interface A2 {
    default void bar1() {}
    default void foo() {}
  }
  
  interface A1 extends A2{
    default void foo(int a) {}
  }

  interface B1 extends A1 {
    default void bar() {}
    default String bar4(int a, int b) {}
    default Integer bar2(int a, int b, String c) {}
  }

  class C1 {
    B1 b1 = new B1() { // Compliant
      @Override
      public void bar() {
        System.out.println();
      }
    };
  }

  class C2 {
    B1 b1 = new B1() { // Compliant
      @Override
      public void foo() {
        System.out.println();
      }
    };
  }

  interface F extends B1{
    default String bar(int a, int b) {}
    default Integer bar2(int a, int b, String c) {}
    default Integer bar3 (int a, int b) {}
  }

  class C3{
    B1 b1 = new B1() {  // Noncompliant
      @Override
      public String bar(int c, int f) {
        return "";
      }
    };

    B1 b2 = new B1() {  // Noncompliant
      @Override
      public Integer bar2(int a, int b, String c) {
        return "";
      }
    };

    F b3 = new F() {  // Compliant
      @Override
      public Integer bar3(int a, int b) {
        return 0;
      }
    };
  }
}
