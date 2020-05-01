package checks;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OneExpectedCheckedExceptionCheck {

  private final Class<IOException> myException = IOException.class;
  private final Executable exec = () -> throwIOException2(throwIOException(1));

  @Test
  public void testG() {
    // Do you expect g() or f() throwing the exception?
    assertThrows(IOException.class, () -> throwIOException2(throwIOException(1)) ); // Noncompliant
    assertThrows(IOException.class, () -> throwIOException2(throwIOException(1)), "Message"); // Noncompliant
    assertThrows(IOException.class, () -> throwIOException2(throwIOException(1)), () -> "message"); // Noncompliant
    assertThrows(IOException.class, () -> { // Noncompliant [[sc=5;ec=17;secondary=25,26] {{Refactor the code of this assertThrows in order to have only one invocation throwing an expected exception.}}
        if (throwIOException2(1) ==
          throwIOException(1)) {}
      } );
    assertThrows(IOException.class, () -> // Noncompliant [[sc=5;ec=17;secondary=29,30]]
      new ThrowingIOException(
        throwIOException(1)
      ) );
    org.junit.Assert.assertThrows(IOException.class, () -> throwIOException2(throwIOException(1)) ); // Noncompliant
    org.junit.Assert.assertThrows("Message", IOException.class, () -> throwIOException2(throwIOException(1)) ); // Noncompliant
    assertThrows(Exception.class, () -> throwException(throwIOException(1)) ); // Noncompliant
    assertThrows(Exception.class, () -> throwIOAndOtherException(throwIOException(1)) ); // Noncompliant

    assertThrows(Exception.class, () -> throwIOAndOtherException(1) ); // Compliant, only one method can throw
    assertThrows(IOException.class, () -> throwIOException(1)); // Compliant, only one method can throw IOException
    assertThrows(IOException.class, () -> throwNothing(throwIOException2(1))); // Compliant, only one method can throw IOException
    assertThrows(IOException.class, () -> throwIOException(throwException(1))); // Compliant, only one method can throw IOException
    assertThrows(IllegalStateException.class, () -> {throwRuntimeException();throwRuntimeException();}); // Compliant, not a checked exception

    assertThrows(myException, () -> throwIOException2(throwIOException(1))); // Compliant, FN
    assertThrows(this.myException, () -> throwIOException2(throwIOException(1))); // Compliant, FN
    assertThrows(IOException.class, exec); // Compliant, FN
    assertThrows(IOException.class, exec); // Compliant, FN
    assertEquals(IOException.class, (Executable)(() -> throwIOException2(throwIOException(1))) ); // Compliant
  }

  @Test
  public void testGTryCatchIdiom() {
    try { // Noncompliant [[sc=5;ec=8;secondary=53,54]] {{Refactor the body of this try/catch in order to have only one invocation throwing an expected exception.}}
      throwIOException2(
        throwIOException(1)
      );
      Assert.fail("Expected an IOException to be thrown");
    } catch (IOException e) {
      // Test exception message...
    }

    try { // Noncompliant
      throwIOException2(throwIOException(1));
      Assert.fail("Expected an IOException to be thrown");
    } catch (IllegalStateException e) { // IllegalStateException is a RuntimeException
    } catch (IOException e) {
    }

    try { // Noncompliant
      throwIOException2(1);
      throwExecutionException(1);
      Assert.fail("Expected an IOException to be thrown");
    } catch (IOException e) {
    } catch (ExecutionException e) {
    }

    try { // Noncompliant
      throwIOException(throwException(1));
      org.junit.jupiter.api.Assertions.fail();
    } catch (Exception e) {
      // Test exception message...
    }

    try { // Compliant, only one method can throw IOException
      throwNothing(throwIOException2(1));
      Assert.fail("Expected an IOException to be thrown");
    } catch (IOException e) {
      // Test exception message...
    }

    try { // Compliant, not a checked exception
      throwRuntimeException();
      throwRuntimeException();
      Assert.fail("Expected an IOException to be thrown");
    } catch (IllegalStateException e) { // IllegalStateException is a RuntimeException
    }

    try { // Compliant, not a checked exception
      throwError();
      throwError();
      Assert.fail("Expected an IOException to be thrown");
    } catch (Error e) { // Error not checked
    }

    try { // Compliant, not a try catch idiom
      throwIOException2(throwIOException(1));
    } catch (IOException e) {
    }

    try { // Compliant, not a try catch idiom
      throwIOException2(throwIOException(1));
      boolean fail = true;
    } catch (IOException e) {
    }

    try { // Compliant, not a try catch idiom
      throwIOException2(throwIOException(1));
      boolean fail;
      fail = true;
    } catch (IOException e) {
    }

    try { // Compliant
      // Empty
    } catch (IllegalStateException e) {
    }

    try { // Compliant, only one method can actually raise the exception.
      throwIOException(1);
      class Nested {
        void f() throws IOException {
          throwIOException2(1);
        }
      }
      assertNull((Executable)(() -> throwIOException2(throwIOException(1))));
      Assert.fail("Expected an IOException to be thrown");
    } catch (IOException e) {
    }
  }

  int throwIOException(int x) throws IOException {
    return x;
  }

  int throwIOException2(int x) throws IOException {
    return x;
  }

  int throwExecutionException(int x) throws ExecutionException {
    return x;
  }

  int throwIOAndOtherException(int x) throws IOException, ExecutionException {
    return x;
  }

  int throwNothing(int x) {
    return x;
  }

  int throwException(int x) throws Exception {
    return x;
  }

  void throwRuntimeException() throws IllegalStateException {
  }

  void throwError() throws Error { // Error are unchecked
    throw new Error();
  }

  class ThrowingIOException {
    ThrowingIOException(int i) throws IOException {

    }
  }
}
