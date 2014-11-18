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
  void foo(){}
}
