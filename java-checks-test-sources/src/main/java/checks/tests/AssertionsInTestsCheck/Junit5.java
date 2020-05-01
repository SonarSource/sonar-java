package checks.tests.AssertionsInTestsCheck;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class Junit5Test {

  @Test
  void test_method_parent() {
    assertTrue(true);
  }
}

class ATest extends Junit5Test {
  @Override
  void test_method_parent() { // Ok - not considered as test method as it is overridden
  }

  @Test
  void test_1_no_assertion() { // Noncompliant {{Add at least one assertion to this test case.}}
  }

  @Test
  void test_1_with_assertion() {
    assertTrue(true);
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "b"})
  void test_2_no_assertion(String val) { // Noncompliant
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "b"})
  void test_2_with_assertion(String val) {
    Assertions.fail("message");
  }

  @RepeatedTest(3)
  void test_3_no_assertion() { // Noncompliant
  }

  @RepeatedTest(3)
  void test_3_with_assertion() {
    Assertions.assertEquals(1, 2);
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

  @TestTemplate
  @ExtendWith(CustomStringContextProvider.class)
  void test_5_no_assertion(String providedString) { // Noncompliant
  }

  @TestTemplate
  @ExtendWith(CustomStringContextProvider.class)
  void test_5_with_assertion(String providedString) {
    bar();
  }

  void bar() {
    Assertions.assertThrows(null, null);
  }

  interface CustomStringContextProvider extends Extension {
  }

}
