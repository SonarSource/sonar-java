package org.foo.bar;

import javax.annotation.Nullable;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MyBarTest {

  @Test
  public void testWithoutAssertions() {
    MyBar myBar = new MyBar();
    Object o = new Object();
    myBar.bar(o, o);

    // squid:S2970: incomplete assertion
    assertThat(myBar);

    foo(o, o);
  }

  // squid:S1172: o1 not used
  private void foo(Object o1, @Nullable Object o2) {
    // squid:S2259: NPE
    o2.toString();
  }

}
