import java.util.List;
class Outer {
  class A {
  }
  class B extends A {
  }
  List<String> foo() {
    Object obj;
    Object o1 = (List<String>) foo(); //NonCompliant
    Object o2 = (List<? extends String>) foo(); //NonCompliant
    Object o3 = (List<? super String>) foo(); //NonCompliant
    String s1 = (String) obj; //Compliant
    String s2 = (String) s1; //NonCompliant
    A a = (A) new B();
    A[][] as = (A[][]) new B[1][1];
    B b;
    fun(b);
    fun((A)b);
    List<B> bees = new java.util.ArrayList<B>();
    java.util.List<A> aaas = (java.util.List) bees;
    int a = 1;
    int b = 2;
    double d = (double) a / (double) b;
    int c = (int)a;
    int e = (int) d;
    obj = (Plop<String>) bar;
    C c = new C((A)null);
  }
  void fun(A a) {
  }

  void fun(B b) {
  }

  class C {
    C(A a) {}
    C(B a) {
      Object o = (Object) fun().newInstance();
    }
    Class fun() { return null;}
  }
}