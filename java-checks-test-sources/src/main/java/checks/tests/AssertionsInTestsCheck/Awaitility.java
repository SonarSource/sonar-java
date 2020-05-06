package checks.tests.AssertionsInTestsCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwaitilityTest {

  private static final int TIMEOUT = 5;
  private List<User> userRepository = new ArrayList<>();

  private Callable<Boolean> newUserIsAdded() {
    return () -> userRepository.size() == 1; // The condition that must be fulfilled
  }

  private interface User {
    static boolean doSomething() {
      return true;
    }
  }

  @Test
  void test1() { // Compliant
    await().until(newUserIsAdded());
  }

  @Test
  void test2() { // Compliant
    await().until(() -> userRepository.size() == 1);
  }

  @Test
  void test3() { // Compliant
    await().atMost(TIMEOUT, TimeUnit.SECONDS).until(newUserIsAdded());
  }

  void test4() { // Compliant
    await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> assertEquals(1, userRepository.size()));
  }

  @Test
  void uncompleted() { // Noncompliant
    await().atMost(TIMEOUT, TimeUnit.SECONDS);
  }

  // control methods
  @Test
  void no_assertions() { // Noncompliant
  }

  @Test
  void junit_assertions() { // Compliant
    assertTrue(User.doSomething(), "should be true");
  }
}
