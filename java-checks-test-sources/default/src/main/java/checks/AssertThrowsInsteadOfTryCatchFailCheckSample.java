package checks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AssertThrowsInsteadOfTryCatchFailCheckSample {
  @Test
  void tests() {
    // NON-COMPLIANT CODE EXAMPLES
    try { // Noncompliant {{Use assertThrows() instead of try/catch/fail() to test that an exception is thrown.}}
      raise();
      fail();
    } catch (Exception _) {
      // test passed
    }

    try { // Noncompliant {{Use assertDoesNotThrow() instead of try/catch/fail() to test that no exception is thrown.}}
      dontRaise();
    } catch (Exception _) {
      fail();
    }

    // COMPLIANT CODE EXAMPLES
    assertThrows(IllegalStateException.class, AssertThrowsInsteadOfTryCatchFailCheckSample::raise);
    assertDoesNotThrow(AssertThrowsInsteadOfTryCatchFailCheckSample::dontRaise);
  }

  private static void raise() {
    throw new IllegalStateException();
  }

  private static void dontRaise(){
    // do nothing
  }
}
