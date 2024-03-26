package checks;

import static org.junit.Assert.assertEquals;

public class AssertionsInProductionCodeCheckSample {

  void method_with_forbidden_assertions() {
    assertEquals(0, 0); // Noncompliant [[sc=5;ec=23;secondary=9,10,11,12,13,14,15]] {{Remove this assertion from production code.}}
    org.junit.Assert.fail("message");
    org.junit.jupiter.api.Assertions.assertFalse(false);
    org.junit.jupiter.api.Assertions.fail("message");
    org.assertj.core.api.Assertions.assertThat(2).isLessThan(3);
    org.assertj.core.api.Assertions.fail("message");
    org.assertj.core.api.Fail.fail("message");
    org.fest.assertions.Assertions.assertThat(2).isEqualTo(2);
  }

  long method_without_forbidden_assertions(int param) {
    assert param == 3;
    return System.currentTimeMillis() - param;
  }

}
