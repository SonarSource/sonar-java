package checks.tests;

import static org.assertj.core.api.Assertions.assertThatObject;
import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.withinPercentage;

class AssertionArgumentOrderCheck {
  static final String CONSTANT = "";
  void junit() {
    assertEquals(0, new AssertionArgumentOrderCheck().actual());
    assertEquals(new AssertionArgumentOrderCheck().actual(), 0); // Noncompliant [[sc=62;ec=63;secondary=12]]
    assertEquals("message", new AssertionArgumentOrderCheck().actual(), 0); // Noncompliant
    assertEquals("message", 0, new AssertionArgumentOrderCheck().actual());
    assertEquals("message", "constantString", actualObject());
    assertEquals("message", actualObject(), "constantString"); // Noncompliant
    assertSame(0, new AssertionArgumentOrderCheck().actual());
    assertSame(new AssertionArgumentOrderCheck().actual(), 0); // Noncompliant
    assertNotSame(new AssertionArgumentOrderCheck().actual(), 0); // Noncompliant
    assertNotSame("message", new AssertionArgumentOrderCheck().actual(), 0); // Noncompliant

    //assert equals with double/float and delta
    assertEquals("message", 0d, actualDouble(), 1d);
    assertEquals(0d, actualDouble(), 1d);
    assertEquals(actualDouble(), 0.0d, 1d); // Noncompliant {{Swap these 2 arguments so they are in the correct order: expected value, actual value.}}
    assertEquals("message", actualDouble(), 0.0d, 1d); // Noncompliant
    assertEquals(actualDouble(), 0.0d); // Noncompliant

    assertEquals("message", 0f, actualFloat(), 1f);
    assertEquals(0f, actualFloat(), 1f);
    assertEquals(actualFloat(), 0.0f, 1f); // Noncompliant {{Swap these 2 arguments so they are in the correct order: expected value, actual value.}}
    assertEquals("message", actualFloat(), 0.0f, 1f); // Noncompliant
    assertEquals(actualFloat(), 0.0f); // Noncompliant


    assertEquals("message", actualObject(), CONSTANT); // Noncompliant
    assertEquals("message", actualObject(), AssertionArgumentOrderCheck.CONSTANT); // Noncompliant
    assertEquals("message", AssertionArgumentOrderCheck.CONSTANT, actualObject());

    assertEquals(1, 2); // Noncompliant {{Change this assertion to not compare two literals.}}
    assertEquals(actualObject(), Boolean.FALSE); // Noncompliant
    assertEquals(Boolean.FALSE, actualObject());

    assertEquals(MyBean.MY_CST, 123); // Noncompliant
    assertEquals(123, MyBean.MY_CST); // Compliant, actual is a constant and expected is a literal (testing a constant in a class)
    assertEquals(actualObject(), ConstantUtils.MY_CONSTANT); // Noncompliant
    assertEquals(ConstantUtils.MY_CONSTANT, MyBean.MY_CST); // Compliant, comparing two constants
  }

  void assertJ() {
    // Simple cases, we can find the expected value
    assertThat(0).isEqualTo(new AssertionArgumentOrderCheck().actual()); // Noncompliant [[sc=16;ec=17;secondary=52]] {{Swap these 2 arguments so they are in the correct order: actual value, expected value.}}
    assertThat(new AssertionArgumentOrderCheck().actual()).isEqualTo(0);
    assertThat("a").isEqualTo("b"); // Noncompliant {{Change this assertion to not compare two literals.}}
    assertThat(actualObject()).isEqualTo("constantString");
    assertThat("constantString").isEqualTo(actualObject()); // Noncompliant
    assertThat(0).isLessThanOrEqualTo(actualObject()); // Noncompliant
    assertThat(CONSTANT).isEqualTo(actualObject()); // Noncompliant
    assertThat(CONSTANT).isEqualTo(123); // Compliant, CONSTANT can store something you want to test, not the best pattern, but still acceptable in this context.
    assertThat(MyBean.MY_CST).isEqualTo(actualObject()); // Noncompliant
    assertThat(MyBean.MY_CST).isEqualTo(123); // Compliant, testing a constant
    assertThat(MyBean.MY_CST).isEqualTo(ConstantUtils.MY_CONSTANT); // Compliant, testing two constant
    assertThat(123).isEqualTo(ConstantUtils.MY_CONSTANT); // Noncompliant

    // More "complex" case, we don't have the expected value, report only when the actual is a literal
    assertThat("constantString").as("message").isEqualTo(actualObject()); // Noncompliant {{Replace this literal with the actual expression you want to assert.}}
    assertThat(0.1).isCloseTo(actualObject(), withinPercentage(11)); // Noncompliant
    assertThat(ConstantUtils.MY_CONSTANT).isCloseTo(actualObject(), withinPercentage(11)); // Compliant
    assertThat(ConstantUtils.MY_CONSTANT).as("message").isEqualTo(actualObject()); // Compliant
    assertThat(ConstantUtils.MY_CONSTANT).isEqualTo(CONSTANT); // Compliant
    assertThat("constantString"); // Noncompliant

    assertThatObject(2).isEqualTo(actualObject()); // Noncompliant
    assertThatObject(actualObject()).isEqualTo(3); // Compliant

  }

  int actual() {
    return 0;
  }
  double actualDouble() {
    return 0;
  }
  float actualFloat() {
    return 0;
  }
  int actualObject() {
    return 0;
  }

  public void testMethod() throws Exception {
    MyBean bean = new MyBean();

    assertEquals(1, bean.getDouble(), 0);
    assertEquals("no FP here", 1, bean.getDouble(), 0);
  }

  static class MyBean {
    static final int MY_CST = 123;

    public double getDouble() {
      return 1;
    }
  }
}

class ConstantUtils {
  static final int MY_CONSTANT = 123;
}
