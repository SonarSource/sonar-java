package checks;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OneExpectedCheckExceptionCheck {

  private final Class<IOException> myException = IOException.class;
  private final Executable exec = () -> throwIOException2(throwIOException(1));

  @Test
  public void testG() {
    // Do you expect g() or f() throwing the exception?
    assertThrows(IOException.class, () -> throwIOException2(throwIOException(1)) ); // Noncompliant
    assertThrows(IOException.class, () -> throwIOException2(throwIOException(1)), "Message"); // Noncompliant
    assertThrows(IOException.class, () -> throwIOException2(throwIOException(1)), () -> "message"); // Noncompliant
    assertThrows(IOException.class, () -> { // Noncompliant [[sc=18;ec=29;secondary=25,26]] {{The tested checked exception can be raised from multiples call, it is unclear what is really tested.}}
        if (throwIOException2(1) ==
          throwIOException(1)) {}
      } );
    org.junit.Assert.assertThrows(IOException.class, () -> throwIOException2(throwIOException(1)) ); // Noncompliant
    org.junit.Assert.assertThrows("Message", IOException.class, () -> throwIOException2(throwIOException(1)) ); // Noncompliant
    assertThrows(Exception.class, () -> throwException(throwIOException(1)) ); // Noncompliant
    assertThrows(Exception.class, () -> throwIOAndOtherException(throwIOException(1)) ); // Noncompliant

    assertThrows(Exception.class, () -> throwIOAndOtherException(1) ); // Compliant, only one method can throw
    assertThrows(IOException.class, () -> throwIOException(1)); // Compliant, only one method can throw IOException
    assertThrows(IOException.class, () -> throwNothing(throwIOException2(1))); // Compliant, only one method can throw IOException
    assertThrows(IOException.class, () -> throwIOException(throwException(1))); // Compliant, only one method can throw IOException

    assertThrows(myException, () -> throwIOException2(throwIOException(1))); // Compliant, FN
    assertThrows(this.myException, () -> throwIOException2(throwIOException(1))); // Compliant, FN
    assertThrows(IOException.class, exec); // Compliant, FN
    assertThrows(IOException.class, exec); // Compliant, FN
    assertEquals(IOException.class, (Executable)(() -> throwIOException2(throwIOException(1))) ); // Compliant
  }

  @Test
  public void testGTryCatchIdiom() {
    try {
      throwIOException2(throwIOException(1));
      Assert.fail("Expected an IOException to be thrown");
    } catch (IOException e) { // Noncompliant
      // Test exception message...
    }

    try {
      throwIOException2(
        throwIOException(1)
      );
      Assert.fail("Expected an IOException to be thrown");
    } catch (IllegalStateException e) {  // Compliant, unchecked exception
    } catch (IOException e) { // Noncompliant [[sc=14;ec=25;secondary=55,56]] {{The tested checked exception can be raised from multiples call, it is unclear what is really tested.}}
    }

    try {
      throwIOException(throwException(1));
      org.junit.jupiter.api.Assertions.fail();
    } catch (Exception e) { // Noncompliant
      // Test exception message...
    }

    try {
      throwNothing(throwIOException2(1));
      Assert.fail("Expected an IOException to be thrown");
    } catch (IOException e) { // Compliant, only one method can throw IOException
      // Test exception message...
    }

    try {
      throwIOException2(throwIOException(1));
    } catch (IOException e) { // Compliant, not a try catch idiom
    }

    try {
      throwIOException2(throwIOException(1));
      boolean fail = true;
    } catch (IOException e) { // Compliant, not a try catch idiom
    }

    try {
      throwIOException2(throwIOException(1));
      boolean fail;
      fail = true;
    } catch (IOException e) { // Compliant, not a try catch idiom
    }

    try {
      // Empty
    } catch (IllegalStateException e) { // Compliant
    }

    try {
      throwIOException(1);
      class Nested {
        void f() throws IOException {
          throwIOException2(1);
        }
      }
      assertNull((Executable)(() -> throwIOException2(throwIOException(1))));
      Assert.fail("Expected an IOException to be thrown");
    } catch (IOException e) { // Compliant, only one method can actually raise the exception.
    }
  }

  int throwIOException(int x) throws IOException {
    return x;
  }

  int throwIOException2(int x) throws IOException {
    return x;
  }

  int throwIOAndOtherException(int x) throws IOException, AccessDeniedException {
    return x;
  }

  int throwNothing(int x) {
    return x;
  }

  int throwException(int x) throws Exception {
    return x;
  }
}
