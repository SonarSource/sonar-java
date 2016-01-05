
class A {
  void foo(Object a) {}
}

class B extends A {
  void foo(Unknown b) {}
}

interface Iiii {
  void bar();
}

class C extends Unknown implements UnknownI, Iiii {
  void foo(Object a) {}
  void bar() {}
  void notAnywhere() {}
}

class D extends A {
  void foo(Object a) {}
  void doesNotOverride() {}
}

class E {
  void foo(Unknown a, Object b) {}
  void foo(Object a, Unknown b) {}
}

class F extends E {
  void foo(UnknownF a, UnknownF2 b) {}
}
