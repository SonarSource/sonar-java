package checks.tests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AssertThrowsInsteadOfTryCatchFailCheckSample {
  @Test
  void tests() {
    try {
      raise();
      fail(); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}} [[quickfixes=qf1]]
//    ^^^^^^
    } catch (Exception _) {
      // test passed
    }
    // fix@qf1 {{Use assertThrows() instead of try/catch and fail() in the try block.}}
    // edit@qf1 [[sl=10;sc=5;el=10;ec=8]] {{assertThrows(Exception.class, () -> }}
    // edit@qf1 [[sl=12;sc=7;el=12;ec=14]] {{}}
    // edit@qf1 [[sl=14;sc=7;el=16;ec=6]] {{);}}

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
      org.assertj.core.api.Fail.fail("expected exception"); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}  [[quickfixes=qf2]]
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception _) {
      // test passed
    }
    // fix@qf2 {{Use assertThrows() instead of try/catch and fail() in the try block.}}
    // edit@qf2 [[sl=53;sc=5;el=53;ec=8]] {{assertThatCode(() -> }}
    // edit@qf2 [[sl=55;sc=7;el=55;ec=60]] {{}}
    // edit@qf2 [[sl=57;sc=7;el=59;ec=6]] {{).withFailMessage("expected exception").isInstanceOf(Exception.class);}}

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


    try {
      raise();
      fail(() -> "expected exception");  // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception _) {
      // test passed
    }

    try {
      raise();
      fail(() -> "expected exception");  // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } finally {}

    // Try with resources : no catch no finally
    try (java.io.StringReader _ = new java.io.StringReader("data")) {
      fail("failed");// Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}} [[quickfixes=qf3]]
//    ^^^^^^^^^^^^^^
    }
    // fix@qf3 {{Use assertThrows() instead of try/catch and fail() in the try block.}}
    // edit@qf3 [[sl=113;sc=5;el=113;ec=8]] {{assertThrows(Throwable.class, () -> }}
    // edit@qf3 [[sl=114;sc=7;el=114;ec=22]] {{}}
    // edit@qf3 [[sl=116;sc=6;el=116;ec=6]] {{, "failed");}}

    // assertJ fail without argument, available since assertJ 3.26.0
    try {
      org.assertj.core.api.Fail.fail(); // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    } catch (Exception e) {
      // test pass
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
      fail("expected exception"); // TN - junit5 fail in junit4 test
    } catch (Exception _) {
      // test passed
    }
    try {
      org.junit.Assert.fail("expected exception"); // TN - junit4 fail in junit4 test
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
