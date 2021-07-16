package org.foo;

record Foo(int a, String s) {
  Foo {
    assert a > 42;
    assert s.length() > 42;
  }
}
