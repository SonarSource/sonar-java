package checks;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.function.Executable;

public class UnreachableCatchCheck {
  void unreachable(boolean cond) {
    try {
      throwExtendsCustomException();
    } catch (ExtendsCustomException e) {
//  ^^^<
      // ...
    } catch (CustomException e) { // Noncompliant {{Remove or refactor this catch clause because it is unreachable, hidden by previous catch block(s).}}
//    ^^^^^
      // ...
    }

    try {
      throw new ExtendsCustomException();
    } catch (ExtendsCustomException e) {
//  ^^^<
      // ...
    } catch (CustomException|IllegalStateException e) { // Noncompliant {{Remove this type because it is unreachable, hidden by previous catch block(s).}}
//           ^^^^^^^^^^^^^^^
      // ...
    }

    try {
      if (true) throw new ExtendsExtendsCustomException();
      throw new ExtendsOtherExtendsCustomException();
    } catch (ExtendsExtendsCustomException e) {
//  ^^^<
      // ...
    } catch (ExtendsOtherExtendsCustomException e) {
//  ^^^<
      // ...
    } catch (ExtendsCustomException | OtherExtendsCustomException e) { // Noncompliant {{Remove or refactor this catch clause because it is unreachable, hidden by previous catch block(s).}}
//    ^^^^^
      // ...
    }

    try {
      new ThrowingExtendsCustomException();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      throwIOException();
    } catch (IOException e) {
      // ...
    } catch (Exception e) { // Compliant, also catch runtime exception
      // ...
    }

    try {
      throwExtendsExtendsCustomException();
    } catch (ExtendsExtendsCustomException e) {
//  ^^^<
      // ...
    } catch (ExtendsCustomException e) { // Noncompliant
//  ^^^<
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      throwExtendsCustomException();
    } catch (ExtendsExtendsCustomException e) { // reported as secondary, even if it can not be raised from code
//  ^^^<
      // ...
    } catch (ExtendsCustomException e) {
//  ^^^<
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      throwExtendsCustomException();
      throwIOException();
    } catch (ExtendsCustomException e) {
//  ^^^<
      // ...
    } catch (IOException e) { // Compliant
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      throwExtendsExtendsCustomException();
      throwIOException();
    } catch (ExtendsCustomException | IOException e) {
//  ^^^<
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      throwExtendsCustomException();

      class Inner {
        void f() throws CustomException {
          throwCustomException(); // not in the same scope
        }
      }
      takeObject((Executable)(this::throwCustomException)); // not in the same scope
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      throwCustomException();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Compliant, a CustomException is thrown, this is reachable
      // ...
    }

    try {
      throw new CustomException();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // compliant
      // ...
    }

    try {
      throwCustomException();
    } catch (ExtendsExtendsCustomException e) {
      // ...
    } catch (ExtendsCustomException e) { // Compliant, throwCustomException can throw one of his subtype
      // ...
    } catch (CustomException e) {
      // ...
    }

    try {
      throw new Exception();
    } catch (ExtendsExtendsCustomException e) {
      // ...
    } catch (ExtendsCustomException e) { // Compliant, FN in this case, but Exception could be one of his subtype
      // ...
    } catch (Exception e) {
      // ...
    }

    try {
      throwExtendsExtendsCustomException();
      throw new Exception();
    } catch (ExtendsExtendsCustomException e) {
      // ...
    } catch (ExtendsCustomException e) { // Compliant
      // ...
    } catch (Exception e) {
      // ...
    }

    try {
      throwExtendsCustomException();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (Throwable e) { // Compliant
      // ...
    }

    try {
      if (cond) {
        throwCustomException();
      } else {
        throwExtendsCustomException();
      }
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Compliant, a CustomException is thrown, this is reachable
      // ...
    }

    try {
      if (cond) {
        throwExtendsCustomException();
      } else {
        throwCustomException();
      }
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Compliant
      // ...
    }

    try {
      throwCustomException();
    } catch (CustomException e) { // Compliant
      // ...
    }

    try(InputStream input = new FileInputStream("reportFileName")) {
      throwExtendsCustomException();
    } catch (FileNotFoundException e) {
    } catch (IOException e) { // Compliant, close throws an IOException
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    class B implements Closeable {
      @Override
      public void close() throws FileNotFoundException {
        throw new RuntimeException();
      }
    }

    try (B a = new B()) {
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) { // Noncompliant
      e.printStackTrace();
    }

    try {
      throwIllegalArgumentException();
    } catch (IllegalArgumentException e) {
      // ...
    } catch (Error e) {
      // ...
    } catch (RuntimeException e) { // Compliant, not a checked exception
      // ...
    }

    try {
      throwExtendsCustomException();
      throwOtherExtendsCustomException();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Compliant
      // ...
    }

    try {
      throwBoth();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Compliant
      // ...
    }

  }

  void throwCustomException() throws CustomException {
    throw new CustomException();
  }

  void throwExtendsCustomException() throws ExtendsCustomException {
    throw new ExtendsCustomException();
  }

  void throwBoth() throws ExtendsCustomException, OtherExtendsCustomException {
  }

  void throwOtherExtendsCustomException() throws OtherExtendsCustomException {
    throw new OtherExtendsCustomException();
  }

  void throwExtendsExtendsCustomException() throws ExtendsExtendsCustomException {
    throw new ExtendsExtendsCustomException();
  }

  void throwIOException() throws IOException {
    throw new IOException();
  }

  void throwIllegalArgumentException() throws IllegalArgumentException {
    throw new IllegalArgumentException();
  }

  void takeObject(Object o) {

  }

  public static class CustomException extends Exception {
  }

  public static class ExtendsCustomException extends CustomException {
  }

  public static class OtherExtendsCustomException extends CustomException {
  }

  public static class ExtendsExtendsCustomException extends ExtendsCustomException {
  }

  public static class ExtendsOtherExtendsCustomException extends OtherExtendsCustomException {
  }

  class ThrowingExtendsCustomException {
    ThrowingExtendsCustomException() throws ExtendsCustomException {

    }
  }

}
