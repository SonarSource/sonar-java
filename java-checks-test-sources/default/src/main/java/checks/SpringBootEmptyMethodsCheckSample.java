package checks;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringBootSanityTest {
  // Compliant, first time we encounter a spring sanity test
  @Test
  void contextLoads() {
  }

  // Noncompliant@+2
  @Test
  void unnecessarySecondEmptyMethod() {
  }
}

@SpringBootTest
class OtherSpringBootTest {
  // Noncompliant@+2
  @Test
  void unnecessaryThirdEmptyMethod() {
  }
}
