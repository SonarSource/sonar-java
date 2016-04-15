package org.foo;

class A {
  Integer i;
  Integer j;

  I anonymous = new I() {
    @Override
    public String bar() {
      return null;
    }
  };
}

interface I {
  String bar();
}