import org.junit.Ignore;

import org.junit.Test;

public class HelloWorld {

  @Test(expected = IllegalArgumentException.class) /* Non-Compliant */
  public void foo() {
  }

  @Test /* Compliant */
  public void bar() {

  }

  @Test
  public void baz() {

  }

}
