package checks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AssertThrowsInsteadOfTryCatchFailCheckSample {
  @Test
  void tests() {
    try { // Noncompliant {{Use assertDoesNotThrow() instead of try/catch and fail() in the try block.}}
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
