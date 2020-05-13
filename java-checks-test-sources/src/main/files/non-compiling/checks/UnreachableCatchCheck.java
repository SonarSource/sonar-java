package checks;

public class UnreachableCatchCheck {
  void unreachable(boolean cond) {
    try {
      throwCustomDerivedException();
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Noncompliant
      // ...
    }

    try {
      throwUnknownException();
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) {
      // ...
    }

    try {
      throwUnknownException();
    } catch (CustomDerivedException e) {
      // ...
    } catch (Unknown e) {
      // ...
    }

    try {
      unknown();
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Compliant
      // ...
    }

    try {
      throwCustomDerivedException();
      unknown();
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Compliant, one unknown method
      // ...
    }

    try {
    } catch (CustomDerivedException e) {
      // ...
    } catch (CustomException e) { // Compliant
      // ...
    }

  }

  void throwUnknownException() throws Unknown {
    throw new Unknown();
  }

  void throwCustomDerivedException() throws CustomDerivedException {
    throw new CustomDerivedException();
  }

  public static class CustomException extends Exception {
  }

  public static class CustomDerivedException extends CustomException {
  }

}
