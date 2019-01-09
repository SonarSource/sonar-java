package org.foo.bar;

import javax.annotation.Nullable;

public class MyBar {

  // squid:S1172: o1 not used
  private void foo(Object o1, @Nullable Object o2) {
    // squid:S2259: NPE
    o2.toString();
  }

  public void bar(Object... objects) {
    foo(objects[0], objects[1]);
  }
}
