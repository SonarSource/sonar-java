package checks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AssertThrowsInsteadOfTryCatchFailCheckSample {

  // fix@qf1 {{Use assertThrows() instead of try/catch and fail() in the try block.}}
  // edit@qf1 [[sl=15;sc=4;el=18;ec=5]]{{assertThrows(Exception.class, () -> {\n      raise();\n      fail();\n//    ^^^^^^\n    });}}

  @Test
  void tests() {
    // Noncompliant@+3 {{Use assertThrows() instead of try/catch and fail() in the try block.}} [[quickfixes=qf1]]
    try {
      raise();
      fail();
//    ^^^^^^
    } catch (Exception _) {
      // test passed
      Runnable x  = () -> {
        System.out.println("hgello"); };
    }
    try {
      dontRaise();
    } catch (Exception _) {
      fail(); // Noncompliant {{Use assertDoesNotThrow() instead of try/catch and fail() in the catch block.}}
//    ^^^^^^
    }

    try {
      raise();
      org.junit.Assert.fail("expected exception"); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception _) {
      // test passed
    }

    try {
      raise();
      junit.framework.Assert.fail("expected exception"); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception _) {
      // test passed
    }

    try {
      raise();
      org.fest.assertions.Fail.fail("expected exception"); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception _) {
      // test passed
    }

    try {
      raise();
      org.assertj.core.api.Fail.fail("expected exception"); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception _) {
      // test passed
    }

    try {
      raise();
      org.assertj.core.api.Assertions.fail("expected exception"); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception _) {
      // test passed
    }

    try {
      raise();
      org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception _) {
      // test passed
    }

    assertThrows(IllegalStateException.class, AssertThrowsInsteadOfTryCatchFailCheckSample::raise); // compliant
    assertDoesNotThrow(AssertThrowsInsteadOfTryCatchFailCheckSample::dontRaise); // compliant
  }

  private static void raise() {
    throw new IllegalStateException();
  }

  private static void dontRaise() {
    // do nothing
  }
}
