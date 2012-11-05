import org.junit.Ignore;

import org.junit.Test;

public class HelloWorld {

  @Test
  @Ignore /* Non-Compliant */
  public void foo() {
  }

  @Test
  @Ignore("message") /* Compliant */
  public void bar() {

  }

  @Test
  public void baz() {

  }

}
