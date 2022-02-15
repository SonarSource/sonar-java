package checks.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

abstract class AssertionArgumentOrderCheck_QuickFixes {

  void foo() {
    // Noncompliant@+1 [[sc=16;ec=32;quickfixes=!]] {{Replace this literal with the actual expression you want to assert.}}
    assertThat("constantString");

    // Noncompliant@+1 [[sc=21;ec=22;quickfixes=!]] {{Change this assertion to not compare two literals.}}
    assertEquals(1, 2);

    // Noncompliant@+1 [[sc=34;ec=38;quickfixes=qf1]] {{Swap these 2 arguments so they are in the correct order: expected value, actual value.}}
    assertEquals(actualDouble(), 0.0d, 1d);
    // fix@qf1 {{Swap arguments}}
    // edit@qf1 [[sc=18;ec=32]] {{0.0d}}
    // edit@qf1 [[sc=34;ec=38]] {{actualDouble()}}

    // Noncompliant@+1 [[sc=16;ec=19;quickfixes=qf2]] {{Swap these 2 arguments so they are in the correct order: actual value, expected value.}}
    assertThat(123).isEqualTo(ConstantUtils.MY_CONSTANT);
    // fix@qf2 {{Swap arguments}}
    // edit@qf2 [[sc=31;ec=56]] {{123}}
    // edit@qf2 [[sc=16;ec=19]] {{ConstantUtils.MY_CONSTANT}}

    // Noncompliant@+1 [[sc=16;ec=32;quickfixes=qf3]] {{Swap these 2 arguments so they are in the correct order: actual value, expected value.}}
    assertThat("constantString")
      .isEqualTo(actualObject());
    // fix@qf3 {{Swap arguments}}
    // edit@qf3 [[sl=+1;sc=18;el=+1;ec=32]] {{"constantString"}}
    // edit@qf3 [[sc=16;ec=32]] {{actualObject()}}

    // Noncompliant@+1 [[sc=16;ec=27;quickfixes=qf4]] {{Swap these 2 arguments so they are in the correct order: actual value, expected value.}}
    assertThat(this.MY_CST).isEqualTo(actualObject());
    // fix@qf4 {{Swap arguments}}
    // edit@qf4 [[sc=39;ec=53]] {{this.MY_CST}}
    // edit@qf4 [[sc=16;ec=27]] {{actualObject()}}

    // Noncompliant@+4 [[sc=7;ec=13;quickfixes=qf5]] {{Swap these 2 arguments so they are in the correct order: expected value, actual value.}}
    assertEquals(
      "not expected",
      actualObject(),
      new A()
        .foo()
        .CST);
    // fix@qf5 {{Swap arguments}}
    // edit@qf5 [[sl=-1;sc=7;el=-1;ec=21]] {{new A()\n        .foo()\n        .CST}}
    // edit@qf5 [[sl=+0;sc=7;el=+2;ec=13]] {{actualObject()}}
  }

  abstract String actualObject();
  abstract double actualDouble();

  private static final String MY_CST = "";

  private static class A {
    A foo() {
      return this;
    }
    private static final String CST = "";
  }

}
