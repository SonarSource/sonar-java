class Outer {
  class Inner {
    Inner(int a){}
  }
}

class Foo {
  void foo(Outer outer) {
    Inner inner = outer.new Inner(2);
  }
}

class Outer2<T> {
  class Inner2 {
    Inner2(T a){}
  }
}

class Foo2 {
  void foo(Outer2<String> outer) {
    Inner2 inner = outer.new Inner2("");
  }
}

class Outer3 {
  class Inner3<T> {
    Inner3() {}
    T bar() { return null; }
  }
}

class Foo3 {
  void foo(Outer3 outer) {
    gul(outer.new Inner3<String>().bar());
  }

  void gul(String s) {}
  void gul(Object o) {}
}