package checks;

import java.io.IOException;
import org.junit.jupiter.api.function.Executable;

public class UnreachableCatchCheck {

  void unreachable(boolean cond) {
    try {
      throwCustomDerivedException();
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Noncompliant [[sc=7;ec=12;secondary=11] {{Remove this catch block because it is unreachable as hidden by previous catch blocks.}}
      // ...
    }

    try {
      throw new CustomDerivedException();
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      new ThrowingCustomDerivedException();
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      throwIOException();
    } catch (IOException e) {
      // ...
    } catch (Exception e) { // Noncompliant
      // ...
    }

    try {
      throwCustomDerivedDerivedException();
    } catch (CustomDerivedDerivedException e) {
      // ...
    } catch (CustomDerivedException e) { // Noncompliant [[secondary=43]]
      // ...
    } catch (CustomException e) { // Noncompliant [[secondary=43,45]]
      // ...
    }

    try {
      throwCustomDerivedException();
    } catch (CustomDerivedDerivedException e) {
      // ...
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Noncompliant [[secondary=55]]
      // ...
    }

    try {
      throwCustomDerivedException();
      throwIOException();
    } catch (CustomDerivedException e) {
      // ...
    } catch (IOException e) { // Compliant
      // ...
    } catch (CustomException e) { // Noncompliant [[sc=7;ec=12;secondary=64]]
      // ...
    }

    try {
      throwCustomDerivedDerivedException();
      throwIOException();
    } catch (CustomDerivedException | IOException e) {
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      throwCustomDerivedException();

      class Inner {
        void f() throws CustomException {
          throwCustomException(); // not in the same scope
        }
      }
      takeObject((Executable)(() -> throwCustomException())); // not in the same scope
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      throwCustomException();
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Compliant, a CustomException is thrown, this is reachable
      // ...
    }

    try {
      throw new CustomException();
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // compliant
      // ...
    }


    try {
      throwCustomDerivedException();
    } catch (CustomDerivedException e) {
      // ...
    } catch (Throwable e) { // Compliant
      // ...
    }

    try {
      if (cond) {
        throwCustomException();
      } else {
        throwCustomDerivedException();
      }
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Compliant, a CustomException is thrown, this is reachable
      // ...
    }

    try {
      if (cond) {
        throwCustomDerivedException();
      } else {
        throwCustomException();
      }
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Compliant
      // ...
    }

    try {
      throwCustomException();
    } catch (CustomException e) { // Compliant
      // ...
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

  }

  void throwCustomException() throws CustomException {
    throw new CustomException();
  }

  void throwCustomDerivedException() throws CustomDerivedException {
    throw new CustomDerivedException();
  }

  void throwCustomDerivedDerivedException() throws CustomDerivedDerivedException {
    throw new CustomDerivedDerivedException();
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

  public static class CustomDerivedException extends CustomException {
  }

  public static class CustomDerivedDerivedException extends CustomDerivedException {
  }

  class ThrowingCustomDerivedException {
    ThrowingCustomDerivedException() throws CustomDerivedException {

    }
  }

}
