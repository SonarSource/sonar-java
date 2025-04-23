package checks.unused;


public class UnusedLocalVariableCheck_withLambda {

  public void subjectTryWithResources() {
    try (org.assertj.core.api.AutoCloseableSoftAssertions softlyTry = new org.assertj.core.api.AutoCloseableSoftAssertions()) { // Noncompliant
    }
  }

  public void subjectEnhancedFor() {
    for (org.assertj.core.api.AutoCloseableSoftAssertions softlyFor : new org.assertj.core.api.AutoCloseableSoftAssertions[]{}) { // Noncompliant
    }
  }

  // Commenting out decoy methods, changes the outcome of the tests without semantics.

  public void decoyTryWithResources() {
    org.assertj.core.api.SoftAssertions.assertSoftly(softlyTry -> {
      softlyTry.assertThat(5).isLessThan(3);
    });
  }

  public void decoyEnhancedFor() {
    org.assertj.core.api.SoftAssertions.assertSoftly(softlyFor -> {
      softlyFor.assertThat(5).isLessThan(3);
    });
  }
}
