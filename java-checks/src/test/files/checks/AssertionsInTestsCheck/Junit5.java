import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class Junit5 {
  @org.junit.jupiter.api.Test
  void test_method_parent() {
    assertTrue(true);
  }
}

class ATest extends Junit5 {
  void test_method_parent() { // Ok - not considered as test method as it is overridden
  }

  @org.junit.jupiter.api.Test
  void test_1_no_assertion() { // Noncompliant {{Add at least one assertion to this test case.}}
  }

  @org.junit.jupiter.api.Test
  void test_1_with_assertion() {
    assertTrue(true);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"a", "b"})
  void test_2_no_assertion(String val) { // Noncompliant
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"a", "b"})
  void test_2_with_assertion(String val) {
    org.junit.jupiter.api.Assertions.fail("message");
  }

  @org.junit.jupiter.api.RepeatedTest(3)
  void test_3_no_assertion() { // Noncompliant
  }

  @org.junit.jupiter.api.RepeatedTest(3)
  void test_3_with_assertion() {
    org.junit.jupiter.api.Assertions.assertEquals(1, 2);
  }

  @TestFactory
  Collection<DynamicTest> test_4_no_assertion() { // Noncompliant
    return Arrays.asList();
  }

  @TestFactory
  Collection<DynamicTest> test_4_with_assertion() {
    return Arrays.asList(
      dynamicTest("1", () -> assertTrue(true)),
      dynamicTest("2", () -> assertTrue(false)));
  }

  @org.junit.jupiter.api.TestTemplate
  @org.junit.jupiter.api.extension.ExtendWith(CustomStringContextProvider.class)
  void test_5_no_assertion(String providedString) { // Noncompliant
  }

  @org.junit.jupiter.api.TestTemplate
  @org.junit.jupiter.api.extension.ExtendWith(CustomStringContextProvider.class)
  void test_5_with_assertion(String providedString) {
    bar();
  }

  void bar() {
    org.junit.jupiter.api.Assertions.assertThrows(null, null);
  }

}
