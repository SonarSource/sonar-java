class Outer {
  class A {
  }
  class B extends A {
  }

  List<String> foo() {
    Object obj;
    Object o1 = (List<String>) foo(); //NonCompliant - false negative because of generics
    Object o2 = (List<? extends String>) foo(); //NonCompliant - false negative because of generics
    Object o3 = (List<? super String>) foo(); //NonCompliant - false negative because of generics
    String s1 = (String) obj; //Compliant
    String s2 = (String) s1; //NonCompliant
    A a = (A) new B();
    A[][] as = (A[][]) new B[1][1];
    B b;
    fun(b);
    fun((A)b);
  }
  void fun(A a) {
  }

  void fun(B b) {
  }

}