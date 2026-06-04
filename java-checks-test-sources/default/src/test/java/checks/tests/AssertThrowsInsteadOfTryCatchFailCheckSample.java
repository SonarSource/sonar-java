package checks.tests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AssertThrowsInsteadOfTryCatchFailCheckSample {
  @Test
  void tests() {
    try {
      raise();
      fail(); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^
    } catch (Exception _) {
      // test passed
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

    try {
      raise();
      org.assertj.core.api.AssertionsForClassTypes.fail("expected exception"); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception _) {
      // test passed
    }

    try {
      raise();
      org.assertj.core.api.AssertionsForInterfaceTypes.fail("expected exception"); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception _) {
      // test passed
    }

    assertThrows(IllegalStateException.class, AssertThrowsInsteadOfTryCatchFailCheckSample::raise); // compliant
    assertDoesNotThrow(AssertThrowsInsteadOfTryCatchFailCheckSample::dontRaise); // compliant
    nonAnnotatedFunctionFN();
  }

  private void nonAnnotatedFunctionFN() {
    org.assertj.core.api.AssertionsForInterfaceTypes.fail("expected exception"); // FN
  }

  @org.junit.Test
  public void junit4AnnotationDontRaise() {
    try {
      fail("expected exception"); // TN - junit5 assert in junit4 test
    } catch (Exception _) {
      // test passed
    }
  }

  private static void raise() {
    throw new IllegalStateException();
  }

  private static void dontRaise() {
    // do nothing
  }
}
