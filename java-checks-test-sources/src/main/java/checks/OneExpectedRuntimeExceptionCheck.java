package checks;

import java.io.IOException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OneExpectedRuntimeExceptionCheck {

  private final Class<IllegalStateException> myException = IllegalStateException.class;
  private final Executable exec = () -> foo(foo(1));

  @Test
  public void testG() {
    // Do you expect g() or f() throwing the exception?
    assertThrows(IllegalStateException.class, () -> foo(foo(1)) ); // Noncompliant
    assertThrows(IllegalStateException.class, () -> foo(foo(1)), "Message"); // Noncompliant
    assertThrows(IllegalStateException.class, () -> foo(foo(1)), () -> "message"); // Noncompliant
    assertThrows(IllegalStateException.class, () -> { // Noncompliant [[sc=5;ec=17;secondary=23,24] {{Refactor the code of this assertThrows to have only one invocation throwing an exception.}}
      if (foo(1) ==
        foo(1)) {}
    } );
    assertThrows(IllegalStateException.class, () -> // Noncompliant [[sc=5;ec=17;secondary=27,28]]
      new NestedClass(
        foo(1)
      ) );
    org.junit.Assert.assertThrows(IllegalStateException.class, () -> foo(foo(1)) ); // Noncompliant
    org.junit.Assert.assertThrows("Message", IllegalStateException.class, () -> foo(foo(1)) ); // Noncompliant
    assertThrows(RuntimeException.class, () -> foo(foo(1)) ); // Noncompliant
    assertThrows(RuntimeException.class, () -> throwCheckedException(throwRuntimeException(1)) ); // Noncompliant

    assertThrows(RuntimeException.class, () -> foo(1) ); // Compliant, only one method can throw

    assertThrows(IOException.class, () -> {throwCheckedException(1);throwCheckedException(1);}); // Compliant, not an unchecked exception

    assertThrows(myException, () -> foo(foo(1))); // Compliant, FN
    assertThrows(this.myException, () -> foo(foo(1))); // Compliant, FN
    assertThrows(IllegalStateException.class, exec); // Compliant, FN
    assertThrows(IllegalStateException.class, exec); // Compliant, FN
  }

  @Test
  public void testGTryCatchIdiom() {
    try { // Noncompliant [[sc=5;ec=8;secondary=48,49]] {{Refactor the body of this try/catch to have only one invocation throwing an exception.}}
      foo(
        foo(1)
      );
      Assert.fail("Expected an IllegalStateException to be thrown");
    } catch (IllegalStateException e) {
      // Test exception message...
    }

    try { // Noncompliant
      foo(foo(1));
      org.junit.jupiter.api.Assertions.fail();
    } catch (Error e) {
      // Test exception message...
    }

    try { // Noncompliant
      foo(1);
      throwCheckedException(1);
      org.assertj.core.api.Fail.fail("Expected an IOException or a IllegalStateException to be thrown");
    } catch (IllegalStateException e) {
    } catch (IOException e) {
    }

    try { // Noncompliant
      foo(1);
      throwCheckedException(1);
      org.assertj.core.api.Assertions.fail("Expected an IOException or a IllegalStateException to be thrown");
    } catch (IllegalStateException e) {
    } catch (IOException e) {
    }

    try { // Noncompliant
      foo(1);
      throwCheckedException(1);
      org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown(IOException.class);
    } catch (IllegalStateException e) {
    } catch (IOException e) {
    }

    try { // Compliant, not a unchecked exception
      throwCheckedException(1);
      throwCheckedException(1);
      Assert.fail("Expected an IOException to be thrown");
    } catch (IOException e) {
    }

    try { // Compliant, only one method can throw IOException
      foo(1);
      Assert.fail("Expected an IllegalStateException to be thrown");
    } catch (IllegalStateException e) {
      // Test exception message...
    }

    try { // Compliant, not a try catch idiom
      foo(foo(1));
    } catch (IllegalStateException e) {
    }

    try { // Compliant, not a try catch idiom
      foo(foo(1));
      boolean fail = true;
    } catch (IllegalStateException e) {
    }

    try { // Compliant, not a try catch idiom
      foo(foo(1));
      boolean fail;
      fail = true;
    } catch (IllegalStateException e) {
    }

    try { // Compliant
      // Empty
    } catch (IllegalStateException e) {
    }

    try { // Compliant, only one method (assertNull) can actually raise the exception.
      class Nested {
        void f() throws IOException {
          foo(1);
        }
      }
      assertNull((Executable)(() -> foo(foo(1))));
      Assert.fail("Expected an IllegalStateException to be thrown");
    } catch (IllegalStateException e) {
    }
  }

  int foo(int x) {
    return x;
  }

  int throwRuntimeException(int x) throws IllegalStateException {
    return x;
  }

  int throwCheckedException(int x) throws IOException {
    return x;
  }

  class NestedClass {
    NestedClass(int i) {

    }

    void foo() {

    }
  }

}
