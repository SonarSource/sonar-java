package checks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AssertThrowsInsteadOfTryCatchFailCheckSample {
  @Test
  void tests() {
    try { // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//      ^[el=+4;ec=5]
      raise();
      fail();
    } catch (Exception _) {
      // test passed
    }

    try {
      dontRaise();
    } catch (Exception _) { // Noncompliant {{Use assertDoesNotThrow() instead of try/catch and fail() in the catch block.}}
//                        ^[el=+3;ec=5]
      fail();
    }

    try { // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//      ^[el=+4;ec=5]
      raise();
      org.junit.Assert.fail("expected exception");
    } catch (Exception _) {
      // test passed
    }

    try { // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//      ^[el=+4;ec=5]
      raise();
      junit.framework.Assert.fail("expected exception");
    } catch (Exception _) {
      // test passed
    }

    try { // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//      ^[el=+4;ec=5]
      raise();
      org.fest.assertions.Fail.fail("expected exception");
    } catch (Exception _) {
      // test passed
    }

    try { // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//      ^[el=+4;ec=5]
      raise();
      org.assertj.core.api.Fail.fail("expected exception");
    } catch (Exception _) {
      // test passed
    }

    try { // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//      ^[el=+4;ec=5]
      raise();
      org.assertj.core.api.Assertions.fail("expected exception");
    } catch (Exception _) {
      // test passed
    }

    try { // Noncompliant {{Use assertThrows() instead of try/catch and fail() in the try block.}}
//      ^[el=+4;ec=5]
      raise();
      org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (Exception _) {
      // test passed
    }

    assertThrows(IllegalStateException.class, AssertThrowsInsteadOfTryCatchFailCheckSample::raise); // compliant
    assertDoesNotThrow(AssertThrowsInsteadOfTryCatchFailCheckSample::dontRaise); // non-compliant
  }

  private static void raise() {
    throw new IllegalStateException();
  }

  private static void dontRaise() {
    // do nothing
  }
}
