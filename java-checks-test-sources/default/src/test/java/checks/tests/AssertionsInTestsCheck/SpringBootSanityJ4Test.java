package checks.tests.AssertionsInTestsCheck;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;

@RunWith(Enclosed.class)
@SpringBootTest
public class SpringBootSanityJ4Test {

  @Test
  public void contextLoads() { // Compliant, no assertions needed for this spring sanity test
  }

  @Test
  public void anotherTest(){ // Noncompliant

  }

  public static class NotASpringBootSanityJ4Test {

    @Test
    public void contextLoads() { // Noncompliant
    }

  }

}


