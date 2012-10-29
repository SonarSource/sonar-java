import org.junit.Test;

public class HelloWorld {

  @Test
  public void foo() {
    assertThat(this.getBoolean(true)).isEqualTo(true); /* Non-Compliant */
    assertThat(this.getBoolean(true)).isTrue();
  }

  private boolean getBoolean(boolean result) {
    return result;
  }

}
