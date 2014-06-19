class A extends B{
  void doSomething(int a, int b) {     // "b" is unused
    compute(a);
  }

  void doSomething(int a) {
    compute(a);
  }

  @Override
  void doSomethingElse(int a, int b) {     // no issue reported on b
    compute(a);
  }
}

class B {
  void doSomethingElse(int a, int b) {
    compute(a);
    compute(b);
  }
  void compute(int a){
    a++;
  }
}

class C extends B {
  int bar;
  void doSomethingElse(int a, int b) {     // no issue reported on b
    compute(a);
  }
  void foo(int a) {
    compute(a);
  }
}

class D extends C {
  void foo(int b, int a) { //false negative
  }
}

class E extends C {
  void bar(int a){ }
}

interface inter {
  default void foo(int a) {
    compute(a);
  }
  default void bar(int a) {
  }
  void qix(int a);
}
class F {
  public static void main(String[] args) { }
  public static int main(String[] args) { }
  public static void main(int[] args) { }
  public static Object main(String[] args) { }
  public static void main(String args) { }
  public static void main(Double[] args) { }
}