package checks.tests;

import java.time.Duration;
import java.util.ArrayList;
import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

public class AssertionInTryCatchCheckSample {

  @Test
  public void test_non_compliant1() {
    // Test secondary issue location
    try {
      throwAssertionError(); // This test will pass even if we comment this line!
      org.junit.Assert.fail("Expected an AssertionError!"); // Noncompliant {{Don't use fail() inside a try-catch catching an AssertionError.}}
//                     ^^^^
    } catch (AssertionError e) {}
//  ^^^<
  }

  @Test
  public void test_non_compliant2() {
    try {
      throwAssertionError(); // This test will pass even if we comment this line!
      fail("Expected an AssertionError!"); // Noncompliant
//    ^^^^
    } catch (IllegalStateException e) {

    } catch (AssertionError e) {}
//  ^^^<
  }

  @Test
  public void test_non_compliant3() {
    // Test other type of error catching AssertionError
    try {
      fail("Expected an AssertionError!", new IllegalArgumentException()); // Noncompliant
    } catch (AssertionFailedError e) {} // AssertionFailedError is a subtype of AssertionError and thrown by JUnit 5
  }

  @Test
  public void test_non_compliant4() {
    try {
      fail(); // Noncompliant
    } catch (Error error) {}
  }

  @Test
  public void test_non_compliant5() {
    try {
      org.assertj.core.api.Assertions.fail(""); // Noncompliant
    } catch (Error error) {}
  }

  @Test
  public void test_non_compliant6() {
    try {
      org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown(IllegalArgumentException.class); // Noncompliant
    } catch (Error error) {}
  }

  @Test
  public void test_non_compliant7() {
    try {
      fail(); // Noncompliant
    } catch (Throwable error) {}
  }

  @Test
  public void test_non_compliant8() {
    try {
      fail("Expected an AssertionError!"); // Noncompliant
    } catch (AssertionError|OutOfMemoryError error) {}
  }

  @Test
  public void test_non_compliant9() {
    //Test JUnit 4
    try {
      org.junit.Assert.assertArrayEquals(new String[1], new String[1]); // Noncompliant {{Don't use assertArrayEquals() inside a try-catch catching an AssertionError.}}
      org.junit.Assert.assertEquals(0,0); // Noncompliant {{Don't use assertEquals() inside a try-catch catching an AssertionError.}}
      org.junit.Assert.assertNotEquals(0, 0); // Noncompliant
      org.junit.Assert.assertNotSame(0 ,0); // Noncompliant
      org.junit.Assert.assertSame(0 ,0); // Noncompliant
      org.junit.Assert.assertNull(null); // Noncompliant
      org.junit.Assert.assertNotNull(null); // Noncompliant
      org.junit.Assert.assertFalse(false); // Noncompliant
      org.junit.Assert.assertTrue(false); // Noncompliant
      org.junit.Assert.assertThat(false, is(false)); // Noncompliant
      org.junit.Assert.fail("Expected an AssertionError!"); // Noncompliant
    } catch (AssertionError e) {}
  }

  @Test
  public void test_non_compliant10() {
    //Test JUnit 5
    try {
      Executable executable = new Executable() {
        @Override
        public void execute() throws Throwable {
        }
      };

      assertArrayEquals(new String[1], new String[1]); // Noncompliant
      assertEquals(0,0); // Noncompliant
      assertNotEquals(0, 0); // Noncompliant
      assertNotSame(0 ,0); // Noncompliant
      assertSame(0 ,0); // Noncompliant
      assertNull(null); // Noncompliant
      assertNotNull(null); // Noncompliant
      assertFalse(false); // Noncompliant
      assertTrue(false); // Noncompliant
      assertIterableEquals(new ArrayList<>(), new ArrayList<>()); // Noncompliant
      assertLinesMatch(new ArrayList<>(), new ArrayList<>()); // Noncompliant
      assertAll(executable); // Noncompliant
      assertTimeout(Duration.ZERO, executable); // Noncompliant
      assertTimeoutPreemptively(Duration.ZERO, executable); // Noncompliant
      assertThrows(AssertionError.class, () -> throwAssertionError()); // Noncompliant
      fail("Expected an AssertionError!"); // Noncompliant
    } catch (AssertionError e) {}
  }

  @Test
  public void test_non_compliant11() {
    try {
      fail("Expected an AssertionError!"); // Noncompliant
    } catch (AssertionError error) {
      Object e = "somethingElse";
      Assert.assertThat(e.toString(), is("Assertion error"));
    }
  }

  @Test
  public void test_non_compliant12() {
    try {
      fail("Expected an AssertionError!"); // Noncompliant
    } catch (AssertionError error) {
      System.out.println("An unrelated message");
    }
  }

  @Test
  public void test_non_compliant13() {
    try {
      if (something()) {
        throwAssertionError();
      } else {
        fail("Expected an AssertionError!"); // Noncompliant
      }
    } catch (AssertionError error) {}
  }

  @Test
  public void test_compliant() {
    assertThrows(AssertionError.class, () -> throwAssertionError()); // Compliant

    //JUnit 4
    try {
      org.junit.Assert.fail("Expected an AssertionError!"); // Compliant, we are testing the properties
      org.junit.Assert.assertEquals(0,0); // Compliant
    } catch (AssertionError e) {
      Assert.assertThat(e.getMessage(), is("Assertion error"));
    }

    //JUnit 5
    try {
      fail("Expected an IllegalStateException!"); // Compliant, we are expecting a IllegalStateException
      assertEquals(0,0); // Compliant
    } catch (IllegalStateException e) {
    }

    try {
      throwAssertionError(); // Compliant, we are not using fail in body
    } catch (AssertionError e) {
    }

    try {
      fail("Expected an AssertionError!"); // Compliant, we are testing the properties
    } catch (AssertionError e) {
      Assert.assertThat(e.getMessage(), is("Assertion error"));
    }

    try {
      org.junit.Assert.fail("Expected an AssertionError!"); // Compliant, FN, we are not testing error properties correctly
    } catch (AssertionError e) {
      System.out.println(e.getMessage());
    }

    try {
      Runnable r = Assertions::fail; // Compliant, nested assertions are not reported
      class Nested {
        Void f = fail(); // Compliant
        void f() {
          fail(); // Compliant
        }
      }
    } catch (AssertionError e) {
    }

  }

  private boolean something() {
    return false;
  }

  private void throwAssertionError() {
    throw new AssertionError("Assertion error");
  }

}
