package java.lang;
// no sema on java.lang
class A {

  int foo(int u) {
    Object a = new Object();
    System.out.println(a);
    a = null;
  }

}
