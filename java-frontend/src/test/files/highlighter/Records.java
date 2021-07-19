package org.foo;

record Foo(int a, String s) {
  static int record;
  Foo {
    assert a > 42;
    assert s.length() > 42;
  }
}
