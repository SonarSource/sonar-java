package org.foo;

import org.apache.commons.lang.StringUtils;

abstract class A {
  void test(String s) {
    boolean a, b, c;

    a = foo();
    b = bar();
    c = StringUtils.isBlank(s);
  }

  static boolean foo() {
    return true;
  }

  abstract boolean bar();
}
