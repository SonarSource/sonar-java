import org.junit.Test;

public class HelloWorld {

  @Test
  public void foo() {
    boolean foo = true;

    assertThat(foo).isEqualTo(true); /* Non-Compliant */
    assertThat(foo).isTrue();
  }

}
