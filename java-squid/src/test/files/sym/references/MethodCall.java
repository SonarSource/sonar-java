package references;

@SuppressWarnings("all")
class MethodCall extends Parent {

  void target() {
  }

  void method() {
    target();
    foo();
  }

}

class Parent {
  void foo() {
  }
}

class C1 {
  void fun(String a) {
  }
}

class C2 extends C1 {
  void fun(Object a) {
  }

  void test() {
    fun("");
  }
}
