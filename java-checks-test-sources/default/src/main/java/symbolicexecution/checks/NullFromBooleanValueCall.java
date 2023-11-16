package symbolicexecution.checks;


class NullFromBooleanValueCall {
  private static void f(String nullableValue) {
    Boolean isValid = true;
    if (nullableValue == null) {
      isValid = false;
    }
    if (isValid.booleanValue()) {
      System.out.println(nullableValue.toLowerCase());
    }
  }

  private static void g(String nullableValue) {
    if (getIsValid().booleanValue()) {
      System.out.println(nullableValue.toLowerCase());
    }
  }

  private static Boolean getIsValid() {
    Boolean isValid = false;
    return  isValid;
  }
}
