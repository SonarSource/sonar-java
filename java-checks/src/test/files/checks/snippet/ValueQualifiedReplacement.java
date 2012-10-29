import org.junit.Test;

public class HelloWorld {

  private boolean foo = true;

  @Test
  public void foo() {
    assertThat(this.foo).isEqualTo(true); /* Non-Compliant */
    assertThat(this.foo).isTrue();
  }

}
