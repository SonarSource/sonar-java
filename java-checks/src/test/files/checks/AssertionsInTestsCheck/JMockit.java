import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Test;

public class AssertionsInTestsCheck2Test {

  class ObjectToBeMocked {
    public int doSomething(String anyString) {
      return 123;
    }
  }

  @Mocked
  ObjectToBeMocked mocked;

  @Test
  public void test_with_verification() {
    new Expectations() {
      {
        mocked.doSomething(anyString);
        result = 123;
      }
    };

    mocked.doSomething("abc");

    new Verifications() {
      {
        mocked.doSomething(anyString);
        times = 1;
      }
    };
    new Double(0.0);
  }

  @Test
  public void test_with_verification_in_helper_method() {
    new Expectations() {
      {
        mocked.doSomething(anyString);
        result = 123;
      }
    };

    mocked.doSomething("abc");

    helper_method();
  }

  @Test
  public void test_no_verification() { // Noncompliant
    new Expectations() {
      {
        mocked.doSomething(anyString);
        result = 123;
      }
    };

    mocked.doSomething("");
  }

  public void helper_method() {
    new Verifications() {
      {
        mocked.doSomething(anyString);
        times = 1;
      }
    };
    new Double(0.0);
  }
}
