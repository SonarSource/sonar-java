import com.google.common.truth.Truth;
import org.junit.Test;

public class AssertionsInTestsCheckTest {
  
  @Test
  public void test() { // Noncompliant
    boolean b = true;
  }

  @Test
  public void test0() { // Compliant
    boolean b = true;
    // do something
    Truth.assertThat(b).isTrue();
  }

  @Test
  public void test1() { // Compliant
    String s = "Hello Truth Framework World!";

    Truth.assertThat(s).contains("Hello");
  }

  @Test
  public void test3() { // Compliant
    boolean b = true;
    Truth.assertThat(b);
  }

  @Test
  public void test4() { // Compliant
    boolean b = true;
    Truth.assertWithMessage("Invalid option").that(b).isFalse();
  }

  @Test
  public void test5() { // Compliant
    boolean b = true;
    Truth.assertWithMessage("Invalid option").that(b);
  }
}
