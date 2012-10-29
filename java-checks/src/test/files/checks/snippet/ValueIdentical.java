import org.junit.Test;

public class HelloWorld {

  @Test
  public void foo() {
    boolean value = true;

    assertThat(value).isEqualTo(true); /* Non-Compliant */
    assertThat(value).isTrue();
  }

}
