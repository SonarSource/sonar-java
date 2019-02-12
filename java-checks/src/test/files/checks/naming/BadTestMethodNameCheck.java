import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class ATest {
  @Test
  void foo() {} // Noncompliant {{Rename this method name to match the regular expression: '^test[A-Z][a-zA-Z0-9]*$'}}

  @Test
  void testFoo() {} // Compliant

  @org.junit.jupiter.api.Test
  void foo_2() {} // Noncompliant {{Rename this method name to match the regular expression: '^test[A-Z][a-zA-Z0-9]*$'}}

  @org.junit.jupiter.api.Test
  void testFoo2() {}

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = { "a", "b" })
  void foo_3(String val) {} // Noncompliant

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = { "a", "b" })
  void testFoo3(String val) {}

  @org.junit.jupiter.api.RepeatedTest(3)
  void foo_4() {} // Noncompliant

  @org.junit.jupiter.api.RepeatedTest(3)
  void testFoo4() {}

  @TestFactory
  Collection<DynamicTest> foo_5() { // Noncompliant
    return Arrays.asList(
      dynamicTest("1", () -> assertTrue(true)),
      dynamicTest("2", () -> assertTrue(false)));
  }

  @TestFactory
  Collection<DynamicTest> testFoo5() {
    return Arrays.asList(
      dynamicTest("1", () -> assertTrue(true)),
      dynamicTest("2", () -> assertTrue(false)));
  }

  @org.junit.jupiter.api.TestTemplate
  @org.junit.jupiter.api.extension.ExtendWith(CustomStringContextProvider.class)
  void foo_6(String providedString) { // Noncompliant
    assertTrue(true);
  }

  @org.junit.jupiter.api.TestTemplate
  @org.junit.jupiter.api.extension.ExtendWith(CustomStringContextProvider.class)
  void testFoo6(String providedString) {
    assertTrue(true);
  }

  void bar() {}
}

class BTest extends ATest {
  @Test
  @Override
  void foo() {} // Compliant
}
