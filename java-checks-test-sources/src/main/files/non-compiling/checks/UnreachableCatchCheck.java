package checks;

public class UnreachableCatchCheck {
  void unreachable(boolean cond) {
    try {
      throwUnknownException();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) {
      // ...
    }

    try {
      throwUnknownException();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (Unknown e) {
      // ...
    }

    try {
      unknown();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Compliant
      // ...
    }

    try {
      unknown();
      throwUnknownException();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Compliant
      // ...
    }

    try {
      throwExtendsCustomException();
      unknown();
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Compliant, one unknown method
      // ...
    }

    try {
    } catch (ExtendsCustomException e) {
      // ...
    } catch (CustomException e) { // Compliant
      // ...
    }

  }

  void throwUnknownException() throws Unknown {
    throw new Unknown();
  }

  void throwExtendsCustomException() throws ExtendsCustomException {
    throw new ExtendsCustomException();
  }

  public static class CustomException extends Exception {
  }

  public static class ExtendsCustomException extends CustomException {
  }

}
