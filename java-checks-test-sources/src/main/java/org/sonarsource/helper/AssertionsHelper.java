package org.sonarsource.helper;

public class AssertionsHelper {

  public class ConstructorAssertion {

  }

  public static void customAssertion() {
    org.assertj.core.api.Assertions.assertThat(true);
  }

  public static void customAssertionAsRuleParameter(boolean expected) {
    // even without assertion inside, if method is defined as a custom assertion method, calling it will not raise any issue for S2699
  }

  public void customInstanceAssertion() {
    org.assertj.core.api.Assertions.assertThat(true);
  }

  public void customInstanceAssertionAsRuleParameter() {
  }

}
