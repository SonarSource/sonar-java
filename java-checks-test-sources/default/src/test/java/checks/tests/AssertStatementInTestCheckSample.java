package checks.tests;

import org.junit.jupiter.api.AfterEach;

public class AssertStatementInTestCheckSample {

  @org.junit.Test
  public void verifiesWithJUnit4() {
    assert Account.activeCount() == 1; // Noncompliant {{Replace this "assert" statement with an assertion from the testing framework.}}
//  ^^^^^^
    org.junit.Assert.assertEquals(1, Account.activeCount());
  }

  @org.junit.jupiter.api.Test
  void verifiesWithJupiter() {
    assert Account.activeCount() == 1 : "an account should be active"; // Noncompliant
//  ^^^^^^
    org.junit.jupiter.api.Assertions.assertEquals(1, Account.activeCount(), "an account should be active");
  }

  @org.testng.annotations.Test
  public void verifiesWithTestNg() {
    assert Account.activeCount() == 1; // Noncompliant
//  ^^^^^^
    org.testng.Assert.assertEquals(Account.activeCount(), 1);
  }

  @org.junit.jupiter.api.Test
  void verifiesInControlFlow() {
    if (Account.activeCount() > 0) {
      assert Account.activeCount() == 1; // Noncompliant
//    ^^^^^^
    }
  }

  @org.junit.jupiter.api.TestTemplate
  void verifiesInLambda() {
    Runnable verification = () -> {
      assert Account.activeCount() == 1; // Noncompliant
//    ^^^^^^
    };
    verification.run();
  }

  @AfterEach
  void resetState() {
    assert Account.activeCount() == 0;
  }

  void helper() {
    assert Account.activeCount() >= 0;
  }

  @Test
  void customTestAnnotationIsIgnored() {
    assert Account.activeCount() == 1;
  }

  @interface Test {
  }

  static class Account {
    static int activeCount() {
      return 1;
    }
  }
}
