package checks.tests.AssertionsInTestsCheck;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringBootSanityTest {

  @Test
  void contextLoads() { // Compliant, no assertions needed for this spring sanity test
  }

  @Test
  void anotherTest(){ // Noncompliant, no assertions

  }

}

class NotASpringBootSanityTest {

  @Test
  void contextLoads() { // Noncompliant
  }

}
